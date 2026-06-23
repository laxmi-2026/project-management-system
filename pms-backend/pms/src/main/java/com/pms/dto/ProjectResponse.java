package com.pms.dto;

import com.pms.model.Project;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * What we send back to the frontend for a Project.
 *
 * Following the exact same DTO pattern used successfully in Task 2's LMS:
 * a record with a static from() factory method that converts the JPA
 * entity into a safe, flat response object. This is what prevents the
 * "ByteBuddyInterceptor" lazy-loading serialization crash that we fixed
 * in Task 2 — we never let a raw entity reach the controller's response.
 */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer progressPercent,
        String createdByUsername,
        LocalDateTime createdAt,
        long totalTasks,
        long completedTasks
) {
    public static ProjectResponse from(Project project, long totalTasks, long completedTasks) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                project.getStatus().name(),
                project.getProgressPercent(),
                project.getCreatedBy() != null ? project.getCreatedBy().getUsername() : null,
                project.getCreatedAt(),
                totalTasks,
                completedTasks
        );
    }

    /** Overload for when task counts aren't needed (e.g. inside a Task's project summary). */
    public static ProjectResponse from(Project project) {
        return from(project, 0, 0);
    }
}
