package com.pms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A small, focused DTO used only by the Kanban drag-and-drop endpoint.
 * When a Member drags a task card from "To Do" to "In Progress", the
 * Angular frontend calls PATCH /api/tasks/{id}/status with just this
 * single field — there's no need to send the whole TaskRequest object
 * for a simple status change.
 */
@Data
public class TaskStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;
}