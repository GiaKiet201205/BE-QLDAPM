package com.example.demo.service;

import com.example.demo.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TeacherSessionAttendanceServiceTest {
    @Test
    void acceptsOnlyTheThreeAgreedStatuses() {
        assertEquals("PRESENT", TeacherSessionAttendanceService.checkedStatus(" present "));
        assertEquals("ABSENT", TeacherSessionAttendanceService.checkedStatus("ABSENT"));
        assertEquals("LATE", TeacherSessionAttendanceService.checkedStatus("late"));
        assertThrows(ApiException.class, () -> TeacherSessionAttendanceService.checkedStatus(null));
        assertThrows(ApiException.class, () -> TeacherSessionAttendanceService.checkedStatus("PENDING"));
        assertThrows(ApiException.class, () -> TeacherSessionAttendanceService.checkedStatus("ASSIGNED"));
        assertThrows(ApiException.class, () -> TeacherSessionAttendanceService.checkedStatus("PAID"));
    }

    @Test
    void cannotRecordBeforeTheLessonStarts() {
        LocalDate date = LocalDate.of(2026, 10, 26);
        LocalTime start = LocalTime.of(14, 0);
        assertFalse(TeacherSessionAttendanceService.lessonHasStarted(
                date, start, LocalDateTime.of(2026, 10, 26, 13, 59)));
        assertTrue(TeacherSessionAttendanceService.lessonHasStarted(
                date, start, LocalDateTime.of(2026, 10, 26, 14, 0)));
        assertTrue(TeacherSessionAttendanceService.lessonHasStarted(
                date, start, LocalDateTime.of(2026, 10, 27, 8, 0)));
    }
}
