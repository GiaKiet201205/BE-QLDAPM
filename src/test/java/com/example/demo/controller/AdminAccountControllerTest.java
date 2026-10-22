package com.example.demo.controller;

import com.example.demo.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AdminAccountControllerTest {
    private final AdminAccountController controller =
        new AdminAccountController(mock(JdbcTemplate.class), new BCryptPasswordEncoder());

    private UsernamePasswordAuthenticationToken actor(String role) {
        return new UsernamePasswordAuthenticationToken("tester", null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void staffCannotReadAccountDirectory() {
        assertThrows(ApiException.class, () -> controller.list(actor("CS")));
    }

    @Test
    void staffCannotCreateOrChangeUsers() {
        var data = new AdminAccountController.CreateAccount(
            "newstaff", "newstaff@example.test", "UniquePass123!",
            "New Staff", "EMP-777", "TEACHER");
        assertThrows(ApiException.class, () -> controller.create(actor("TC"), data));
        assertThrows(ApiException.class,
            () -> controller.changeRole(actor("CM"), "staff", new AdminAccountController.ChangeRole("CS")));
        assertThrows(ApiException.class,
            () -> controller.changeStatus(actor("TEACHER"), "staff", new AdminAccountController.ChangeStatus("LOCKED")));
    }

    @Test
    void cannotCreatePrivilegedAdminRoleViaStaffForm() {
        var data = new AdminAccountController.CreateAccount(
            "newadmin", "admin2@example.test", "UniquePass123!",
            "New Admin", "EMP-778", "ADMIN");
        assertThrows(ApiException.class, () -> controller.create(actor("ADMIN"), data));
    }
}
