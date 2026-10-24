package com.example.demo.controller;

import com.example.demo.service.ClassStaffingRangeService;
import com.example.demo.service.ClassStaffingRangeService.RangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** TC manages teacher coverage; CM manages CS support, independently of payroll. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/class-planning/{classId}/staffing")
public class ClassStaffingRangeController {
    private final ClassStaffingRangeService service;

    @GetMapping
    public List<Map<String,Object>> coverage(@PathVariable String classId, @RequestParam String role) {
        return service.coverage(classId, role);
    }

    @PostMapping
    public Map<String,Object> assignRange(@PathVariable String classId, @RequestBody RangeRequest request) {
        if (request != null && request.role() != null && request.role().isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        return service.assignRange(classId, request);
    }
}
