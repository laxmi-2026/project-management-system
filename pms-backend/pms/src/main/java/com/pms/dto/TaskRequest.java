package com.pms.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * What the frontend sends when creating or updating a Task.
 *
 * VALIDATION RULES:
 *   - title: letters, spaces, hyphens, and apostrophes only — no
 *     digits or other punctuation. This keeps task titles readable
 *     and consistent (e.g. "Design Homepage", "Client's Dashboard"),
 *     and matches the same rule applied to Project names.
 *   - description: optional, capped at 2000 chars to match the DB column
 *   - dueDate: @FutureOrPresent — can never be set in the past, on
 *     either create or edit (see TaskService for the matching
 *     project-start-date cross-check, which layers on top of this)
 */
@Data
public class TaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(max = 150, message = "Task title must be under 150 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Task title can only contain letters, spaces, hyphens, and apostrophes — no numbers")
    private String title;

    @Size(max = 2000, message = "Description must be under 2000 characters")
    private String description;

    @FutureOrPresent(message = "Due date cannot be in the past")
    private LocalDate dueDate;

    /** LOW, MEDIUM, HIGH */
    private String priority;

    /** TODO, IN_PROGRESS, DONE */
    private String status;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    /** Optional — can be left unassigned at creation time. */
    private Long assignedToUserId;
}