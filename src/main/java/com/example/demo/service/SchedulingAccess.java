package com.example.demo.service;

import com.example.demo.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SchedulingAccess {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final JdbcTemplate jdbc;

    public record StaffIdentity(String id, String username, String role) {}

    public StaffIdentity current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        var employees = jdbc.query(
            "SELECT e.employee_id, e.username, u.role_id FROM employees e " +
            "JOIN users u ON e.username=u.username " +
            "WHERE e.username=? AND e.status='ACTIVE' AND u.status='ACTIVE'",
            (rs, row) -> new StaffIdentity(rs.getString(1), rs.getString(2), rs.getString(3)),
            authentication.getName());
        if (employees.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Active employee profile required");
        }
        return employees.get(0);
    }

    public StaffIdentity requireRole(String... roles) {
        StaffIdentity actor = current();
        if (Arrays.stream(roles).noneMatch(role -> role.equalsIgnoreCase(actor.role()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Role is not allowed for this action");
        }
        return actor;
    }

    public void requireEmployeeRole(String employeeId, String role) {
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM employees e JOIN users u ON u.username=e.username " +
            "WHERE e.employee_id=? AND e.status='ACTIVE' AND u.status='ACTIVE' AND u.role_id=?",
            Integer.class, employeeId, role);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Employee not found or has incorrect role");
        }
    }

    public void validTimes(LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be later than start time");
        }
    }

    public LocalDate monday(LocalDate weekStart) {
        LocalDate date = weekStart == null ? LocalDate.now(BUSINESS_ZONE) : weekStart;
        return date.with(DayOfWeek.MONDAY);
    }

    public LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }

    public String day(LocalDate date) {
        return date.getDayOfWeek().name().substring(0, 3).toLowerCase(Locale.ROOT);
    }

    public String normalizedDay(String input) {
        if (input == null) return null;
        String day = input.trim().toLowerCase(Locale.ROOT);
        if (!Arrays.asList("mon", "tue", "wed", "thu", "fri", "sat", "sun").contains(day)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid dayOfWeek");
        }
        return day;
    }

    public void lockEmployee(String employeeId) {
        jdbc.queryForObject("SELECT employee_id FROM employees WHERE employee_id=? FOR UPDATE",
            String.class, employeeId);
    }

    public void audit(StaffIdentity actor, String action, String type, String entityId) {
        jdbc.update("INSERT INTO audit_logs (audit_log_id,username,action,entity_type,entity_id) " +
            "VALUES (?,?,?,?,?)", UUID.randomUUID().toString(), actor.username(),
            action, type, entityId);
    }

    public boolean hasApprovedAvailability(String employeeId, LocalDate date,
                                            LocalTime start, LocalTime end, String campus) {
        // Only an approved slot which fully covers the requested time can be assigned.
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM availabilities WHERE employee_id=? " +
            "AND upper(status)='APPROVED' AND start_time<=? AND end_time>=? " +
            "AND (available_date=? OR (available_date IS NULL AND lower(day_of_week)=?)) " +
            "AND (? IS NULL OR campus IS NULL OR campus='' OR campus=?)",
            Integer.class, employeeId, start, end, date, day(date), campus, campus);
        return count != null && count > 0;
    }

    public String campusForWorkplace(String workplaceId) {
        if (workplaceId == null) return null;
        return switch (workplaceId) {
            case "center-01" -> "Cơ sở chính";
            case "center-02" -> "Cơ sở Quận 1";
            case "center-03" -> "Cơ sở Quận 3";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown workplaceId");
        };
    }

    public Map<String, Object> employeeSummary(String employeeId) {
        return jdbc.queryForMap(
            "SELECT e.employee_id AS id, e.employee_id AS \"employeeId\", " +
            "e.full_name AS \"fullName\", e.employee_code AS \"employeeCode\", " +
            "u.role_id AS role FROM employees e JOIN users u ON e.username=u.username WHERE e.employee_id=?",
            employeeId);
    }
}
