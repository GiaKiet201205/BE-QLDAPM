package com.example.demo.controller;

import com.example.demo.exception.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;

    public record CreateAccount(
        @NotBlank @Size(min=3,max=50) String username,
        @NotBlank @Email @Size(max=100) String email,
        @NotBlank @Size(min=8,max=128) String password,
        @NotBlank @Size(max=100) String fullName,
        @NotBlank @Size(max=30) String employeeCode,
        @NotBlank String role
    ) {}
    public record ChangeRole(@NotBlank String role) {}
    public record ChangeStatus(@NotBlank String status) {}

    private void admin(Authentication auth) {
        if (auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private String staffRole(String value) {
        String role = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("TEACHER", "TC", "CM", "CS", "SALE").contains(role)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported staff role");
        }
        return role;
    }

    private void audit(Authentication actor, String action, String target) {
        db.update("INSERT INTO audit_logs(audit_log_id,username,action,entity_type,entity_id) VALUES (?,?,?,?,?)",
            UUID.randomUUID().toString(), actor.getName(), action, "ACCOUNT", target);
    }

    private List<Map<String,Object>> rows() {
        return db.queryForList(
            "SELECT u.username,u.email,u.role_id AS role,u.status,u.created_at AS \"createdAt\", " +
            "e.employee_id AS \"employeeId\",e.employee_code AS \"employeeCode\", " +
            "e.full_name AS \"fullName\" FROM users u LEFT JOIN employees e ON e.username=u.username " +
            "ORDER BY u.created_at DESC,u.username");
    }

    @GetMapping
    public List<Map<String,Object>> list(Authentication auth) {
        admin(auth);
        return rows();
    }

    @Transactional
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(Authentication auth, @Valid @RequestBody CreateAccount data) {
        admin(auth);
        String role = staffRole(data.role());
        String username = data.username().trim();
        String email = data.email().trim();
        String code = data.employeeCode().trim();
        if (db.queryForObject("SELECT count(*) FROM users WHERE lower(username)=lower(?) OR lower(email)=lower(?)",
                Integer.class,username,email) > 0 ||
            db.queryForObject("SELECT count(*) FROM employees WHERE lower(employee_code)=lower(?)",
                Integer.class,code) > 0) {
            throw new ApiException(HttpStatus.CONFLICT,"Username, email or employee code already exists");
        }
        db.update("INSERT INTO users(username,email,password_hash,role_id,status) VALUES (?,?,?,?,'ACTIVE')",
                username,email,passwords.encode(data.password()),role);
        db.update("INSERT INTO employees(employee_id,username,employee_code,full_name,status) VALUES (?,?,?,?,'ACTIVE')",
                UUID.randomUUID().toString(),username,code,data.fullName().trim());
        audit(auth, "CREATE_EMPLOYEE_ACCOUNT", username);
        return rows().stream().filter(row -> username.equals(row.get("username"))).findFirst().orElseThrow();
    }

    @Transactional
    @PatchMapping("/{username}/role")
    public Map<String,Object> changeRole(Authentication auth, @PathVariable String username,
                                         @Valid @RequestBody ChangeRole data) {
        admin(auth);
        String next = staffRole(data.role());
        if (db.queryForObject("SELECT count(*) FROM users WHERE username=? AND role_id<>'ADMIN'",
                Integer.class,username) == 0 ||
            db.queryForObject("SELECT count(*) FROM employees WHERE username=?",
                Integer.class,username) == 0) {
            throw new ApiException(HttpStatus.CONFLICT,"Only employee roles can be changed");
        }
        Integer activeShifts = db.queryForObject(
            "SELECT (SELECT count(*) FROM teaching_schedules t JOIN employees e " +
            "ON e.employee_id=t.teacher_id WHERE e.username=? AND t.status='ASSIGNED' AND t.date >= CURRENT_DATE) + " +
            "(SELECT count(*) FROM staff_schedules s JOIN employees e " +
            "ON e.employee_id=s.employee_id WHERE e.username=? AND s.status='ASSIGNED' AND s.date >= CURRENT_DATE)",
            Integer.class, username, username);
        if (activeShifts != null && activeShifts > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Cancel or reassign active schedules before changing employee role");
        }
        db.update("UPDATE users SET role_id=? WHERE username=?",next,username);
        audit(auth, "CHANGE_ACCOUNT_ROLE", username);
        return rows().stream().filter(row -> username.equals(row.get("username"))).findFirst().orElseThrow();
    }

    @Transactional
    @PatchMapping("/{username}/status")
    public Map<String,Object> changeStatus(Authentication auth, @PathVariable String username,
                                           @Valid @RequestBody ChangeStatus data) {
        admin(auth);
        String state = data.status().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE","LOCKED").contains(state)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status must be ACTIVE or LOCKED");
        }
        if (username.equalsIgnoreCase(auth.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot lock your own administrator account");
        }
        if (db.update("UPDATE users SET status=? WHERE username=?",state,username) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND,"User not found");
        }
        audit(auth, "CHANGE_ACCOUNT_STATUS_" + state, username);
        return rows().stream().filter(row -> username.equals(row.get("username"))).findFirst().orElseThrow();
    }
}
