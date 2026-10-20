package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class StatisticsDtos {
    private StatisticsDtos() {}

    public record StatisticsResponse(
            String period,
            LocalDate startDate,
            LocalDate endDate,
            Overview overview,
            EmployeeBreakdown employees,
            ScheduleSummary schedules,
            FinancialSummary financial,
            List<MonthlyStatistics> monthly
    ) {}

    public record Overview(long activeStudents, long newStudents, long activeClasses) {}

    public record EmployeeBreakdown(
            long total,
            long teachers,
            long coordinators,
            long centerManagers,
            long sales,
            long customerSupport,
            long admins
    ) {}

    public record ScheduleSummary(
            long teachingAssigned,
            long teachingPending,
            long teachingCancelled,
            long staffAssigned,
            long staffPending,
            long staffCancelled,
            long pendingAvailability,
            long teachingConflicts,
            long staffConflicts
    ) {}

    public record FinancialSummary(
            BigDecimal revenue,
            boolean revenueAvailable,
            BigDecimal salaryCost,
            Long payrollRecords
    ) {}

    public record MonthlyStatistics(
            int month,
            String label,
            long students,
            long newEnrollments,
            long classes,
            BigDecimal revenue,
            BigDecimal salaryCost,
            BigDecimal profit
    ) {}
}
