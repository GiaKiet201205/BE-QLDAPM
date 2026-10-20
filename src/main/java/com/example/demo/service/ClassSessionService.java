package com.example.demo.service;

import com.example.demo.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassSessionService {
    private final JdbcTemplate db;

    public record PlannedSessionInput(LocalDate date, LocalTime startTime, LocalTime endTime) {}
    private record Slot(LocalDate date, LocalTime start, LocalTime end) {}
    private record Identity(String username, String role) {}

    private Identity identity(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String role = auth.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .filter(value -> value.startsWith("ROLE_"))
                .map(value -> value.substring(5))
                .findFirst().orElse("");
        return new Identity(auth.getName(), role);
    }

    private boolean manages(String classId, Identity actor) {
        if ("ADMIN".equals(actor.role())) return true;
        if (!"CS".equals(actor.role())) return false;
        Integer count = db.queryForObject(
            "SELECT count(*) FROM employees e JOIN users u ON u.username=e.username " +
            "WHERE e.username=? AND e.status='ACTIVE' AND u.status='ACTIVE' AND (" +
            "EXISTS(SELECT 1 FROM class_access_scopes a WHERE a.employee_id=e.employee_id AND a.class_id=? AND a.status='ACTIVE') " +
            "OR EXISTS(SELECT 1 FROM staff_schedules s WHERE s.employee_id=e.employee_id AND s.class_id=? " +
            "AND s.work_type='CLASS_SUPPORT' AND s.status='ASSIGNED'))",
            Integer.class, actor.username(), classId, classId);
        return count != null && count > 0;
    }

    private boolean canView(String classId, Identity actor) {
        if (manages(classId, actor) || Set.of("TC", "CM").contains(actor.role())) return true;
        if (!"TEACHER".equals(actor.role())) return false;
        Integer count = db.queryForObject(
            "SELECT count(*) FROM teaching_schedules t JOIN employees e ON t.teacher_id=e.employee_id " +
            "WHERE t.class_id=? AND t.status='ASSIGNED' AND e.username=?",
            Integer.class, classId, actor.username());
        return count != null && count > 0;
    }

    public List<Map<String,Object>> listClassPlanning(Authentication auth) {
        Identity actor = identity(auth);
        if (!Set.of("TC","CM","ADMIN").contains(actor.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Planning list is only for TC, CM or Admin");
        }
        // Exclude student roster and sensitive student details.
        return db.queryForList(
            "SELECT c.class_id AS \"classId\", c.class_code AS \"classCode\", " +
            "c.name, c.start_date AS \"startDate\", c.end_date AS \"endDate\", " +
            "c.status, cr.name AS \"courseName\" " +
            "FROM classes c JOIN courses cr ON cr.course_id=c.course_id " +
            "WHERE c.status IN ('READY','RUNNING') ORDER BY c.start_date,c.class_code");
    }

    public List<Map<String,Object>> sessions(String classId, Authentication auth) {
        Identity actor = identity(auth);
        if (!canView(classId, actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No access to class planned sessions");
        }
        return db.queryForList(
            "SELECT class_session_id AS id, class_id AS \"classId\", date, " +
            "start_time AS \"startTime\", end_time AS \"endTime\", status " +
            "FROM class_sessions WHERE class_id=? ORDER BY date,start_time",
            classId);
    }

    @Transactional
    public List<Map<String,Object>> replace(String classId, List<PlannedSessionInput> input, Authentication auth) {
        Identity actor = identity(auth);
        if (!manages(classId, actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Admin or managing CS can plan class sessions");
        }
        List<Map<String,Object>> classes = db.queryForList(
            "SELECT class_id, start_date, end_date, status FROM classes WHERE class_id=? FOR UPDATE", classId);
        if (classes.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "Class not found");
        Map<String,Object> row = classes.get(0);
        if (!Set.of("DRAFT", "READY").contains(row.get("status"))) {
            throw new ApiException(HttpStatus.CONFLICT, "Plan can only change while class is Draft or Ready");
        }
        if (input == null || input.size() > 400) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Plan must contain at most 400 lessons");
        }
        if ("READY".equals(row.get("status")) && input.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A Ready class must retain at least one planned lesson");
        }
        LocalDate start = ((Date)row.get("start_date")).toLocalDate();
        LocalDate end = ((Date)row.get("end_date")).toLocalDate();
        List<Slot> desired = new ArrayList<>();
        for (PlannedSessionInput item : input) {
            if (item == null || item.date() == null || item.startTime() == null || item.endTime() == null ||
                    !item.endTime().isAfter(item.startTime()) ||
                    item.date().isBefore(start) || item.date().isAfter(end)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Planned lessons must be valid and within the class dates");
            }
            Slot slot = new Slot(item.date(), item.startTime(), item.endTime());
            if (desired.contains(slot)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate lesson in plan");
            }
            for (Slot other : desired) {
                if (other.date().equals(slot.date()) &&
                        other.start().isBefore(slot.end()) && other.end().isAfter(slot.start())) {
                    throw new ApiException(HttpStatus.CONFLICT, "Class lessons cannot overlap");
                }
            }
            desired.add(slot);
        }

        List<Map<String,Object>> assigned = db.queryForList(
            "SELECT date,start_time,end_time FROM teaching_schedules " +
            "WHERE class_id=? AND status='ASSIGNED' UNION ALL " +
            "SELECT date,start_time,end_time FROM staff_schedules " +
            "WHERE class_id=? AND status='ASSIGNED' AND work_type='CLASS_SUPPORT'", classId, classId);
        for (Map<String,Object> existing : assigned) {
            Slot slot = new Slot(((Date)existing.get("date")).toLocalDate(),
                    LocalTime.parse(existing.get("start_time").toString()), LocalTime.parse(existing.get("end_time").toString()));
            if (!desired.contains(slot)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Cannot remove or change a lesson with assigned Teacher/CS. Cancel assignments first.");
            }
        }

        List<Map<String,Object>> previous = db.queryForList(
            "SELECT class_session_id, date,start_time,end_time FROM class_sessions WHERE class_id=? FOR UPDATE", classId);
        for (Map<String,Object> item : previous) {
            Slot slot = new Slot(((Date)item.get("date")).toLocalDate(),
                    LocalTime.parse(item.get("start_time").toString()), LocalTime.parse(item.get("end_time").toString()));
            if (!desired.contains(slot)) db.update("DELETE FROM class_sessions WHERE class_session_id=?",
                    item.get("class_session_id"));
        }
        Set<Slot> old = previous.stream()
                .map(item -> new Slot(((Date)item.get("date")).toLocalDate(),
                        LocalTime.parse(item.get("start_time").toString()), LocalTime.parse(item.get("end_time").toString())))
                .collect(Collectors.toSet());
        for (Slot slot : desired) {
            if (!old.contains(slot)) {
                db.update("INSERT INTO class_sessions (class_session_id,class_id,date,start_time,end_time,status) " +
                        "VALUES (?,?,?,?,?,'PLANNED')",
                        UUID.randomUUID().toString(), classId, slot.date(), slot.start(), slot.end());
            }
        }
        db.update("INSERT INTO audit_logs (audit_log_id,username,action,entity_type,entity_id,description) " +
                "VALUES (?,?, 'UPDATE_CLASS_PLAN','CLASS',?,?)",
                UUID.randomUUID().toString(), actor.username(), classId,
                "Updated planned lessons: " + desired.size());
        return sessions(classId, auth);
    }
}
