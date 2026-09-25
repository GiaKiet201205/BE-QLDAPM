package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    // Endpoint public, khong can token
    @GetMapping("/public/ping")
    public Map<String, String> ping() {
        return Map.of("message", "pong - endpoint nay khong can dang nhap");
    }

    // Endpoint yeu cau token hop le (xem SecurityConfig: anyRequest().authenticated())
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities()
        );
    }
}
