package com.example.demo.service;

import com.example.demo.dto.SchedulingDtos.AvailabilityRequest;
import com.example.demo.dto.SchedulingDtos.ReviewRequest;
import com.example.demo.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final JdbcTemplate db;
    private final SchedulingAccess access;

    private static final String SELECT =
        "SELECT a.availability_id AS id,a.employee_id AS \"employeeId\",e.full_name AS \"fullName\",u.role_id AS role," +
        "a.available_date AS \"availableDate\",a.day_of_week AS day,a.shift," +
        "a.start_time AS start,a.end_time AS \"end\",a.availability_type AS type,a.campus,a.note," +
        "upper(a.status) AS status,a.created_at AS \"submittedAt\",a.reviewed_at AS \"reviewedAt\" " +
        "FROM availabilities a JOIN employees e ON a.employee_id=e.employee_id " +
        "JOIN users u ON e.username=u.username ";

    public List<Map<String,Object>> mine(LocalDate weekStart) {
        var actor = access.requireRole("TEACHER","CS","SALE");
        return db.queryForList(SELECT +
            "WHERE a.employee_id=? AND (a.available_date IS NULL OR a.available_date BETWEEN ? AND ?) ORDER BY a.day_of_week,a.start_time",
            actor.id(), access.monday(weekStart), access.monday(weekStart).plusDays(6));
    }

    public List<Map<String,Object>> list(String role, String status, LocalDate weekStart) {
        var actor = access.requireRole("TC","CM","ADMIN");
        String selectedRole = role == null ? (actor.role().equals("TC") ? "TEACHER" : null) : role.toUpperCase(Locale.ROOT);
        if (selectedRole != null && !List.of("TEACHER","CS","SALE").contains(selectedRole)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
        if (actor.role().equals("TC") && selectedRole != null && !selectedRole.equals("TEACHER")
            || actor.role().equals("CM") && "TEACHER".equals(selectedRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot inspect this role");
        }
        String query = SELECT +
            "WHERE (CAST(? AS varchar) IS NULL OR u.role_id=?) AND (CAST(? AS varchar) IS NULL OR upper(a.status)=?) " +
            "AND (a.available_date IS NULL OR a.available_date BETWEEN ? AND ?) " +
            (actor.role().equals("TC") ? "AND u.role_id='TEACHER' " :
             actor.role().equals("CM") ? "AND u.role_id IN ('SALE','CS') " : "") +
            "ORDER BY e.full_name,a.day_of_week,a.start_time";
        String normalizedStatus = status == null ? null : status.toUpperCase(Locale.ROOT);
        return db.queryForList(query,selectedRole,selectedRole,normalizedStatus,normalizedStatus,
            access.monday(weekStart),access.monday(weekStart).plusDays(6));
    }

    private void validate(AvailabilityRequest body) {
        access.validTimes(body.startTime(),body.endTime());
        String type = body.availabilityType() == null ? "RECURRING" :
            body.availabilityType().toUpperCase(Locale.ROOT);
        if (!List.of("RECURRING","WEEKLY").contains(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"availabilityType must be RECURRING or WEEKLY");
        }
        if ("WEEKLY".equals(type) && body.availableDate() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"availableDate required for WEEKLY availability");
        }
        if ("RECURRING".equals(type) && body.availableDate() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"RECURRING availability cannot include availableDate");
        }
        if (body.availableDate() == null && body.dayOfWeek() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"dayOfWeek required");
        }
        if (body.dayOfWeek() != null && body.availableDate() != null &&
            !access.normalizedDay(body.dayOfWeek()).equals(access.day(body.availableDate()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"dayOfWeek does not match availableDate");
        }
        if (body.note()!=null && body.note().length()>255) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"note maximum length is 255");
        }
    }

    private String day(AvailabilityRequest body) {
        return body.availableDate() == null ? access.normalizedDay(body.dayOfWeek())
            : access.day(body.availableDate());
    }

    private String kind(AvailabilityRequest body) {
        return body.availabilityType() == null ? "RECURRING" :
            body.availabilityType().toUpperCase(Locale.ROOT);
    }

    private Map<String,Object> one(String id) {
        return db.queryForList(SELECT+"WHERE a.availability_id=?",id).stream().findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Availability not found"));
    }

    private void assertNoOverlap(
            String employeeId,
            AvailabilityRequest body,
            String exceptId
    ) {
        Integer count = db.queryForObject(
            "SELECT count(*) " +
            "FROM availabilities " +
            "WHERE employee_id = ? " +
            "AND availability_id <> ? " +
            "AND ( " +
            "    (? IS NULL AND lower(day_of_week) = ?) " +
            "    OR available_date = ? " +
            "    OR (available_date IS NULL AND lower(day_of_week) = ?) " +
            ") " +
            "AND start_time < ? " +
            "AND end_time > ?",
            Integer.class,
            employeeId,
            exceptId,
            body.availableDate(),
            day(body),
            body.availableDate(),
            day(body),
            body.endTime(),
            body.startTime()
        );

        if (count != null && count > 0) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "Availability time overlaps existing slot"
            );
        }
    }

    @Transactional
    public Map<String,Object> create(AvailabilityRequest body) {
        var actor=access.requireRole("TEACHER","CS","SALE");
        validate(body);
        access.lockEmployee(actor.id());
        assertNoOverlap(actor.id(),body,"");
        String id=UUID.randomUUID().toString();
        db.update("INSERT INTO availabilities (availability_id,employee_id,available_date,day_of_week," +
            "shift,start_time,end_time,availability_type,campus,note,status) VALUES (?,?,?,?,?,?,?,?,?,?,'PENDING')",
            id,actor.id(),body.availableDate(),day(body),body.shift(),body.startTime(),body.endTime(),
            kind(body),body.campus(),body.note());
        access.audit(actor,"REGISTER_AVAILABILITY","AVAILABILITY",id);
        return one(id);
    }

    @Transactional
    public Map<String,Object> update(String id, AvailabilityRequest body) {
        var actor=access.requireRole("TEACHER","CS","SALE");
        validate(body);
        access.lockEmployee(actor.id());
        Map<String,Object> old=one(id);
        if (!actor.id().equals(old.get("employeeId"))) {
            throw new ApiException(HttpStatus.FORBIDDEN,"Not your availability");
        }
        assertEditable(id);
        assertNoOverlap(actor.id(),body,id);
        db.update("UPDATE availabilities SET available_date=?,day_of_week=?,shift=?,start_time=?,end_time=?," +
            "availability_type=?,campus=?,note=?,status='PENDING',reviewed_at=NULL,reviewed_by=NULL WHERE availability_id=?",
            body.availableDate(),day(body),body.shift(),body.startTime(),body.endTime(),kind(body),
            body.campus(),body.note(),id);
        access.audit(actor,"UPDATE_AVAILABILITY","AVAILABILITY",id);
        return one(id);
    }

    private void assertEditable(String id) {
        Integer reserved=db.queryForObject(
            "SELECT (SELECT count(*) FROM teaching_schedules t JOIN availabilities a " +
            "ON a.employee_id=t.teacher_id WHERE a.availability_id=? AND t.status='ASSIGNED' " +
            "AND (a.available_date=t.date OR (a.available_date IS NULL AND " +
            "lower(a.day_of_week)=lower(to_char(t.date,'dy')))) " +
            "AND t.start_time>=a.start_time AND t.end_time<=a.end_time) + " +
            "(SELECT count(*) FROM staff_schedules s JOIN availabilities a " +
            "ON a.employee_id=s.employee_id WHERE a.availability_id=? AND s.status='ASSIGNED' " +
            "AND (a.available_date=s.date OR (a.available_date IS NULL AND " +
            "lower(a.day_of_week)=lower(to_char(s.date,'dy')))) " +
            "AND s.start_time>=a.start_time AND s.end_time<=a.end_time)",
            Integer.class,id,id);
        if (reserved!=null && reserved>0) {
            throw new ApiException(HttpStatus.CONFLICT,"Cancel existing assignments before changing this availability");
        }
    }

    @Transactional
    public void delete(String id) {
        var actor=access.requireRole("TEACHER","CS","SALE");
        access.lockEmployee(actor.id());
        if (!actor.id().equals(one(id).get("employeeId"))) {
            throw new ApiException(HttpStatus.FORBIDDEN,"Not your availability");
        }
        assertEditable(id);
        db.update("DELETE FROM availabilities WHERE availability_id=?",id);
        access.audit(actor,"DELETE_AVAILABILITY","AVAILABILITY",id);
    }

    @Transactional
    public Map<String,Object> review(String id,ReviewRequest body) {
        var actor=access.requireRole("TC","CM");
        Map<String,Object> slot=one(id);
        if ("TEACHER".equals(slot.get("role")) && !actor.role().equals("TC") ||
            !"TEACHER".equals(slot.get("role")) && !actor.role().equals("CM")) {
            throw new ApiException(HttpStatus.FORBIDDEN,"Reviewer role not permitted");
        }
        String status=body.status().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED","REJECTED").contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,"status must be APPROVED or REJECTED");
        }
        access.lockEmployee((String)slot.get("employeeId"));
        Map<String,Object> latest=one(id);
        if (!"PENDING".equals(latest.get("status"))) {
            throw new ApiException(HttpStatus.CONFLICT,"Only pending availability can be reviewed");
        }
        db.update("UPDATE availabilities SET status=?,reviewed_at=now(),reviewed_by=? WHERE availability_id=?",
            status,actor.id(),id);
        access.audit(actor,"REVIEW_AVAILABILITY","AVAILABILITY",id);
        return one(id);
    }
}
