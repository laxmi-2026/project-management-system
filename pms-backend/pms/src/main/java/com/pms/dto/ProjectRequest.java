package com.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * What the frontend sends when creating or updating a Project.
 *
 * name: letters, spaces, hyphens, and apostrophes only — no digits.
 * Same rule as Task.title, applied consistently across both entities
 * so the naming convention is uniform throughout the app.
 *
 * No "id" field here — the ID is either auto-generated (on create) or
 * taken from the URL path variable (on update), never trusted from
 * the request body.
 */
@Data
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must be under 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Project name can only contain letters, spaces, hyphens, and apostrophes — no numbers")
    private String name;

    @Size(max = 2000, message = "Description must be under 2000 characters")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    /** ACTIVE, PLANNING, ON_HOLD, COMPLETED — validated again in the service layer. */
    private String status;
}