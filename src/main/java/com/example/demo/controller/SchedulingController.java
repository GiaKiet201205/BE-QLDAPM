package com.example.demo.controller;

import com.example.demo.dto.SchedulingDtos.*;
import com.example.demo.service.AssignmentService;
import com.example.demo.service.AvailabilityService;
import com.example.demo.service.SchedulingAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SchedulingController {
    private final AvailabilityService availability;
    private final AssignmentService assignment;
    private final SchedulingAccess access;

    @GetMapping("/employees")
    public List<Map<String, Object>> employees(@RequestParam String role) {
        return assignment.employees(role);
    }

    @GetMapping("/availabilities/me")
    public List<Map<String, Object>> myAvailability(@RequestParam(required = false) LocalDate weekStart) {
        return availability.mine(weekStart);
    }

    @GetMapping("/availabilities")
    public List<Map<String, Object>> listAvailability(
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) LocalDate weekStart
    ) {
        return availability.list(role, status, weekStart);
    }

    @PostMapping("/availabilities")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody AvailabilityRequest body) {
        return availability.create(body);
    }

    @PutMapping("/availabilities/{id}")
    public Map<String, Object> editAvailability(@PathVariable String id,
                                                 @Valid @RequestBody AvailabilityRequest body) {
        return availability.update(id, body);
    }

    @DeleteMapping("/availabilities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvailability(@PathVariable String id) {
        availability.delete(id);
    }

    @PatchMapping("/availabilities/{id}/review")
    public Map<String, Object> review(@PathVariable String id, @Valid @RequestBody ReviewRequest body) {
        return availability.review(id, body);
    }

    @GetMapping("/teaching-schedules")
    public List<Map<String, Object>> teaching(@RequestParam(required = false) LocalDate weekStart) {
        return assignment.teaching(weekStart);
    }

    @PostMapping("/teaching-schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> assignTeacher(@Valid @RequestBody TeachingRequest body) {
        return assignment.assignTeacher(body);
    }

    @PutMapping("/teaching-schedules/{id}")
    public Map<String, Object> editTeacher(@PathVariable String id, @Valid @RequestBody TeachingRequest body) {
        return assignment.editTeacher(id, body);
    }

    @DeleteMapping("/teaching-schedules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTeacher(@PathVariable String id) {
        assignment.cancelTeacher(id);
    }

    @GetMapping("/staff-schedules")
    public List<Map<String, Object>> staff(@RequestParam(required = false) LocalDate weekStart,
                                           @RequestParam(required = false) String role) {
        return assignment.staff(weekStart, role);
    }

    @PostMapping("/staff-schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> assignStaff(@Valid @RequestBody StaffRequest body) {
        return assignment.assignStaff(body);
    }

    @PutMapping("/staff-schedules/{id}")
    public Map<String, Object> editStaff(@PathVariable String id, @Valid @RequestBody StaffRequest body) {
        return assignment.editStaff(id, body);
    }

    @DeleteMapping("/staff-schedules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelStaff(@PathVariable String id) {
        assignment.cancelStaff(id);
    }

    @GetMapping("/schedules/me")
    public List<Map<String, Object>> mine(@RequestParam(required = false) LocalDate weekStart) {
        return assignment.mine(weekStart);
    }
}
