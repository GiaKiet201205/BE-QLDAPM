package com.example.demo.service;

import com.example.demo.dto.SchedulingDtos.StaffRequest;
import com.example.demo.dto.SchedulingDtos.TeachingRequest;
import com.example.demo.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Select sessions from the class's established timetable, not arbitrary calendar slots.
 * A whole-course assignment creates individual rows in teaching_schedules/staff_schedules:
 * each lesson can subsequently be substituted without rewriting the course timetable.
 */
@Service
@RequiredArgsConstructor
public class ClassStaffingRangeService {
    private static final int MAX_SESSIONS = 400; // same cap as class plan
    private final JdbcTemplate db;
    private final SchedulingAccess access;
    private final AssignmentService assignments;

    public record RangeRequest(String role, String employeeId, String campus,
                               String workplaceId, String mode, List<String> sessionIds) {}

    private String roleAndPermission(String requested) {
        String role = requested == null ? "" : requested.trim().toUpperCase(Locale.ROOT);
        if ("TEACHER".equals(role)) access.requireRole("TC");
        else if ("CS".equals(role)) access.requireRole("CM");
        else throw new ApiException(HttpStatus.BAD_REQUEST, "Role must be TEACHER or CS");
        return role;
    }

    public List<Map<String,Object>> coverage(String classId, String requestedRole) {
        String role = roleAndPermission(requestedRole);
        requireClass(classId);
        String table = "TEACHER".equals(role) ? "teaching_schedules" : "staff_schedules";
        String id = "TEACHER".equals(role) ? "teaching_schedule_id" : "staff_schedule_id";
        String employee = "TEACHER".equals(role) ? "teacher_id" : "employee_id";
        String work = "CS".equals(role) ? " AND z.work_type='CLASS_SUPPORT'" : "";
        return db.queryForList(
            "SELECT s.class_session_id AS id, s.date, " +
            "s.start_time AS \"startTime\", s.end_time AS \"endTime\", " +
            "a." + id + " AS \"assignmentId\", a." + employee + " AS \"employeeId\", " +
            "e.full_name AS \"employeeName\", " +
            "(SELECT count(*) FROM " + table + " x WHERE x.class_id=s.class_id " +
            "AND x.date=s.date AND x.start_time=s.start_time AND x.end_time=s.end_time " +
            "AND x.status='ASSIGNED'" + ("CS".equals(role) ? " AND x.work_type='CLASS_SUPPORT'" : "") +
            ") AS \"assignedCount\", " +
            ("TEACHER".equals(role) ? "ta.status AS \"attendanceStatus\" " :
                                   "NULL::varchar AS \"attendanceStatus\" ") +
            "FROM class_sessions s " +
            "LEFT JOIN LATERAL (SELECT * FROM " + table + " z " +
            "WHERE z.class_id=s.class_id AND z.date=s.date " +
            "AND z.start_time=s.start_time AND z.end_time=s.end_time " +
            "AND z.status='ASSIGNED'" + work + " ORDER BY z.created_at,z." + id + " LIMIT 1) a ON TRUE " +
            "LEFT JOIN employees e ON e.employee_id=a." + employee + " " +
            ("TEACHER".equals(role)
                ? "LEFT JOIN teacher_session_attendance ta ON ta.teaching_schedule_id=a.teaching_schedule_id " : "") +
            "WHERE s.class_id=? AND s.status='PLANNED' ORDER BY s.date,s.start_time",
            classId);
    }

    @Transactional
    public Map<String,Object> assignRange(String classId, RangeRequest request) {
        String role = roleAndPermission(request == null ? null : request.role());
        if (request.employeeId() == null || request.employeeId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "employeeId is required");
        }
        String mode = request.mode() == null ? "FILL_EMPTY" : request.mode().trim().toUpperCase(Locale.ROOT);
        if (!List.of("FILL_EMPTY", "REPLACE").contains(mode)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "mode must be FILL_EMPTY or REPLACE");
        }
        List<String> ids = request.sessionIds();
        if (ids == null || ids.isEmpty() || ids.size() > MAX_SESSIONS ||
                ids.stream().anyMatch(id -> id == null || id.isBlank()) ||
                new HashSet<>(ids).size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose 1-400 unique planned lessons");
        }
        var actor = access.requireRole("TEACHER".equals(role) ? "TC" : "CM");
        access.requireEmployeeRole(request.employeeId(), role);
        requireClass(classId);
        // Class-level lock serializes range changes and keeps an all-or-nothing result.
        db.queryForObject("SELECT class_id FROM classes WHERE class_id=? FOR UPDATE",
                String.class, classId);
        List<String> created = new ArrayList<>();
        int skipped = 0;

        for (String sessionId : ids) {
            List<Map<String,Object>> planned = db.queryForList(
                "SELECT date,start_time,end_time FROM class_sessions " +
                "WHERE class_session_id=? AND class_id=? AND status='PLANNED'",
                sessionId, classId);
            if (planned.isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, "Selected lesson is missing or cancelled: " + sessionId);
            }
            Map<String,Object> lesson = planned.get(0);
            LocalDate date = ((Date)lesson.get("date")).toLocalDate();
            LocalTime start = LocalTime.parse(lesson.get("start_time").toString());
            LocalTime end = LocalTime.parse(lesson.get("end_time").toString());
            String table = "TEACHER".equals(role) ? "teaching_schedules" : "staff_schedules";
            String idColumn = "TEACHER".equals(role) ? "teaching_schedule_id" : "staff_schedule_id";
            String ownerColumn = "TEACHER".equals(role) ? "teacher_id" : "employee_id";
            String workFilter = "CS".equals(role) ? " AND work_type='CLASS_SUPPORT'" : "";
            List<Map<String,Object>> current = db.queryForList(
                "SELECT " + idColumn + " AS id, " + ownerColumn + " AS owner FROM " + table +
                " WHERE class_id=? AND date=? AND start_time=? AND end_time=? " +
                "AND status='ASSIGNED'" + workFilter + " ORDER BY created_at",
                classId, date, start, end);

            boolean owned = current.stream().anyMatch(item -> request.employeeId().equals(item.get("owner")));
            if (owned) {
                if (current.size() > 1 && "REPLACE".equals(mode)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Multiple staff assignments; replace individually: " + date);
                }
                skipped++;
                continue;
            }
            if (!current.isEmpty() && "FILL_EMPTY".equals(mode)) {
                throw new ApiException(HttpStatus.CONFLICT,
                    "Lesson already has staff; use replacement mode: " + date + " " + start);
            }

            if ("REPLACE".equals(mode) && !current.isEmpty()) {
                if (current.size() > 1) {
                    throw new ApiException(HttpStatus.CONFLICT, "Multiple staff assignments; replace individually: " + date);
                }
                String oldId = current.get(0).get("id").toString();
                if (!LocalDateTime.of(date, start).isAfter(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))) {
                    // A previous teacher marked ABSENT can be replaced retroactively,
                    // but never overwrite present/late attendance or CS historical coverage.
                    String oldAttendance = "TEACHER".equals(role) ? db.query(
                        "SELECT status FROM teacher_session_attendance WHERE teaching_schedule_id=?",
                        (rs, n) -> rs.getString(1), oldId
                    ).stream().findFirst().orElse(null) : null;
                    if (!("TEACHER".equals(role) && ids.size() == 1 &&
                            "ABSENT".equals(oldAttendance))) {
                        throw new ApiException(HttpStatus.CONFLICT,
                            "Historical staff replacement is not permitted: " + date);
                    }
                }
                db.update("UPDATE " + table + " SET status='CANCELLED' WHERE " + idColumn +
                    "=? AND status='ASSIGNED'", oldId);
                access.audit(actor, "REPLACE_" + role + "_LESSON",
                        "TEACHER".equals(role) ? "TEACHING_SCHEDULE" : "STAFF_SCHEDULE", oldId);
            }

            if ("TEACHER".equals(role)) {
                var row = assignments.assignTeacher(new TeachingRequest(
                    request.employeeId(), classId, date, start, end, request.campus(), null));
                created.add(row.get("id").toString());
            } else {
                var row = assignments.assignStaff(new StaffRequest(
                    request.employeeId(), "CS", request.workplaceId(), classId,
                    date, start, end, "CLASS_SUPPORT"));
                created.add(row.get("id").toString());
            }
        }
        return Map.of("createdCount", created.size(), "unchangedCount", skipped,
                "createdIds", created);
    }

    private void requireClass(String classId) {
        Integer count = db.queryForObject("SELECT count(*) FROM classes WHERE class_id=? " +
                "AND status IN ('READY','RUNNING')", Integer.class, classId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a Ready or Running class");
        }
    }
}
