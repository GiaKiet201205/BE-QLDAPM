package com.example.demo.controller;

import com.example.demo.service.TeacherSessionAttendanceService;
import com.example.demo.service.TeacherSessionAttendanceService.MarkRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** CS records actual attendance; salary calculation is outside this module. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classes/{classId}/teacher-attendance")
public class TeacherSessionAttendanceController {
    private final TeacherSessionAttendanceService attendance;

    @GetMapping
    public List<Map<String,Object>> list(@PathVariable String classId) {
        return attendance.list(classId);
    }

    @PutMapping("/{teachingScheduleId}")
    public Map<String,Object> mark(@PathVariable String classId,
                                   @PathVariable String teachingScheduleId,
                                   @RequestBody MarkRequest request) {
        return attendance.mark(classId, teachingScheduleId, request);
    }
}
