package com.example.demo.service;

import com.example.demo.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClassDatePolicyTest {
    private final LocalDate originalStart = LocalDate.of(2026, 10, 2);
    private final LocalDate originalEnd = LocalDate.of(2026, 11, 15);

    private void allowed(String status, LocalDate nextStart, LocalDate nextEnd) {
        assertDoesNotThrow(() -> ClassManagementService.enforceDateEditPolicy(
            status, originalStart, originalEnd, nextStart, nextEnd));
    }

    private void blocked(String status, LocalDate nextStart, LocalDate nextEnd) {
        assertThrows(ApiException.class, () -> ClassManagementService.enforceDateEditPolicy(
            status, originalStart, originalEnd, nextStart, nextEnd));
    }

    @Test
    void draftCanChangeEitherDate() {
        allowed("DRAFT", originalStart.plusDays(1), originalEnd.minusDays(1));
    }

    @Test
    void readyCanAdjustRangeSubjectToSeparateLessonHistoryValidation() {
        allowed("READY", originalStart.plusDays(1), originalEnd.plusWeeks(1));
    }

    @Test
    void runningCannotChangeStartButCanExtendOrShortenEnd() {
        blocked("RUNNING", originalStart.plusDays(1), originalEnd);
        allowed("RUNNING", originalStart, originalEnd.plusDays(7));
        allowed("RUNNING", originalStart, originalEnd.minusDays(2));
    }

    @Test
    void completedAndClosedDatesCannotChange() {
        for (String status : new String[] {"COMPLETED", "CLOSED"}) {
            blocked(status, originalStart.plusDays(1), originalEnd);
            blocked(status, originalStart, originalEnd.plusDays(1));
            allowed(status, originalStart, originalEnd);
        }
    }
}
