package com.pms.dto;

import java.util.List;
import java.util.Map;

/**
 * What the dashboard screen receives.
 *
 * The same DTO is returned for every role, but the SERVICE layer fills
 * in different numbers depending on who's asking:
 *   - Admin/Manager: sees totals across ALL projects
 *   - Member: sees totals only for tasks assigned to them
 *
 * tasksByStatus example: { "TODO": 5, "IN_PROGRESS": 3, "DONE": 12 }
 * This map is exactly what the Angular Kanban board uses to draw its
 * three columns and their task counts.
 */
public record DashboardStats(
        long totalProjects,
        long activeProjects,
        long completedProjects,
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        Map<String, Long> tasksByStatus,
        List<TaskResponse> myRecentTasks
) {
}
