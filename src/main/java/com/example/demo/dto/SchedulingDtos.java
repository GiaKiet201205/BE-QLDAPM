package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public final class SchedulingDtos {
    private SchedulingDtos() {}

    // availableDate is required for a one-week registration; a recurring registration
    // uses dayOfWeek (MON..SUN) and leaves availableDate null.
    public record AvailabilityRequest(
        LocalDate availableDate,
        String dayOfWeek,
        String shift,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String availabilityType,
        String campus,
        String note
    ) {}

    public record ReviewRequest(@NotBlank String status) {}

    public record TeachingRequest(
        @NotBlank String teacherId,
        @NotBlank String classId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String campus,
        String room
    ) {}

    // Batch operations are atomic: every selected planned lesson must pass validation.
    public record TeachingBatchRequest(
        @NotBlank String teacherId,
        @NotBlank String classId,
        String campus,
        java.util.List<@NotBlank String> sessionIds
    ) {}

    public record SupportBatchRequest(
        @NotBlank String employeeId,
        @NotBlank String classId,
        String workplaceId,
        java.util.List<@NotBlank String> sessionIds
    ) {}

    public record StaffRequest(
        @NotBlank String employeeId,
        @NotBlank String staffRole,
        String workplaceId,
        String classId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank String workType
    ) {}
}
