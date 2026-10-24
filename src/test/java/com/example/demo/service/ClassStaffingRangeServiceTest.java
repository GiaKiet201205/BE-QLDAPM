package com.example.demo.service;

import com.example.demo.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassStaffingRangeServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final SchedulingAccess access = mock(SchedulingAccess.class);
    private final AssignmentService assignments = mock(AssignmentService.class);
    private final ClassStaffingRangeService service =
            new ClassStaffingRangeService(db, access, assignments);

    @Test
    void rejectsEmptyAndDuplicateLessonSelectionsWithoutCreatingAssignments() {
        var empty = new ClassStaffingRangeService.RangeRequest(
                "TEACHER", "teacher-01", "Cơ sở chính", null, "FILL_EMPTY", List.of());
        assertThrows(ApiException.class, () -> service.assignRange("class-01", empty));

        var duplicates = new ClassStaffingRangeService.RangeRequest(
                "TEACHER", "teacher-01", "Cơ sở chính", null,
                "FILL_EMPTY", List.of("session-01", "session-01"));
        assertThrows(ApiException.class, () -> service.assignRange("class-01", duplicates));

        verify(access, atLeastOnce()).requireRole("TC");
        verifyNoInteractions(assignments);
        verifyNoInteractions(db);
    }

    @Test
    void acceptsAtMostFourHundredSlotsPerTransaction() {
        var oversized = new ClassStaffingRangeService.RangeRequest(
                "CS", "cs-01", null, "center-01", "FILL_EMPTY",
                IntStream.range(0, 401).mapToObj(i -> "lesson-" + i).toList());
        assertThrows(ApiException.class, () -> service.assignRange("class-01", oversized));
        verify(access).requireRole("CM");
        verifyNoInteractions(db, assignments);
    }

    @Test
    void rejectsUnrecognizedRoleAndReplaceMode() {
        var wrongRole = new ClassStaffingRangeService.RangeRequest(
                "SALE", "sale-01", null, null, "FILL_EMPTY", List.of("lesson-01"));
        assertThrows(ApiException.class, () -> service.assignRange("class-01", wrongRole));

        var badMode = new ClassStaffingRangeService.RangeRequest(
                "CS", "cs-01", null, "center-01", "OVERWRITE",
                List.of("lesson-01"));
        assertThrows(ApiException.class, () -> service.assignRange("class-01", badMode));

        verifyNoInteractions(db, assignments);
    }

    @Test
    void rejectsNullRequest() {
        assertThrows(ApiException.class, () -> service.assignRange("class-01", null));
        verifyNoInteractions(db, assignments);
    }
}
