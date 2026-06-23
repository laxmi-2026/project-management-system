package com.pms.dto;

import com.pms.model.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Flat response DTO for a Task — same pattern as Task 2's
 * EnrollmentResponse. Notice projectName and assignedToUsername are
 * flat strings, not nested objects. This is intentional: it directly
 * prevents the exact frontend bug we hit in Task 2 (the
 * "/api/courses/undefined/complete" bug caused by a frontend expecting
 * a nested object that the DTO didn't actually provide). Flat fields
 * remove that entire category of bug before it can happen.
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        String priority,
        String status,
        Long projectId,
        String projectName,
        Long assignedToUserId,
        String assignedToUsername,
        String createdByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean overdue
) {
    public static TaskResponse from(Task task) {
        boolean isOverdue = task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDate.now())
                && task.getStatus() != Task.Status.DONE;

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority().name(),
                task.getStatus().name(),
                task.getProject() != null ? task.getProject().getId() : null,
                task.getProject() != null ? task.getProject().getName() : null,
                task.getAssignedTo() != null ? task.getAssignedTo().getId() : null,
                task.getAssignedTo() != null ? task.getAssignedTo().getUsername() : null,
                task.getCreatedBy() != null ? task.getCreatedBy().getUsername() : null,
                task.getCreatedAt(),
                task.getUpdatedAt(),
                isOverdue
        );
    }
}