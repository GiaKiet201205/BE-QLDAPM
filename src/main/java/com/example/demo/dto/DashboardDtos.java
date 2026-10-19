package com.example.demo.dto;

import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardResponse(
            String role,
            List<Stat> stats,
            String tableTitle,
            List<String> tableColumns,
            List<List<String>> tableRows,
            List<Activity> activities,
            String panelTitle,
            List<PanelItem> panelItems
    ) {}

    public record Stat(String label, String value, String trend, boolean trendUp) {}
    public record Activity(String text, String time) {}
    public record PanelItem(String label, String due, boolean urgent) {}
}
