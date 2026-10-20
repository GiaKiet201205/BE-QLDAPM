package com.example.demo.controller;

import com.example.demo.service.ClassSessionService;
import com.example.demo.service.ClassSessionService.PlannedSessionInput;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ClassSessionController {
    private final ClassSessionService sessions;

    @GetMapping("/class-planning")
    public List<Map<String,Object>> planning(Authentication authentication) {
        return sessions.listClassPlanning(authentication);
    }

    @GetMapping("/classes/{classId}/planned-sessions")
    public List<Map<String,Object>> get(@PathVariable String classId, Authentication authentication) {
        return sessions.sessions(classId, authentication);
    }

    @PutMapping("/classes/{classId}/planned-sessions")
    public List<Map<String,Object>> replace(@PathVariable String classId,
            @RequestBody List<@Valid PlannedSessionInput> sessionsInput,
            Authentication authentication) {
        return sessions.replace(classId, sessionsInput, authentication);
    }
}
