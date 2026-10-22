package com.example.demo.security;

import com.example.demo.service.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsValidTokenWhenAccountIsDisabled() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        CustomUserDetailsService users = mock(CustomUserDetailsService.class);
        when(jwt.extractUsername("test-token")).thenReturn("locked-user");
        when(users.loadUserByUsername("locked-user")).thenReturn(
            User.withUsername("locked-user").password("hash").roles("TEACHER")
                .disabled(true).build());

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        var response = new MockHttpServletResponse();
        new JwtAuthFilter(jwt, users).doFilter(
            request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwt, never()).isTokenValid(anyString(), anyString());
    }

    @Test
    void acceptsValidTokenForEnabledAccount() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        CustomUserDetailsService users = mock(CustomUserDetailsService.class);
        when(jwt.extractUsername("test-token")).thenReturn("active-user");
        when(users.loadUserByUsername("active-user")).thenReturn(
            User.withUsername("active-user").password("hash").roles("CS").build());
        when(jwt.isTokenValid("test-token", "active-user")).thenReturn(true);

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        new JwtAuthFilter(jwt, users).doFilter(
            request, new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
