package com.pms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a Project — the top-level container that holds many Tasks.
 *
 * Example: "Website Redesign" is a Project. Inside it there might be
 * tasks like "Design homepage", "Build login page", "Test checkout flow".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must be under 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * PLANNING   - project created but work hasn't started
     * ACTIVE     - work is currently happening
     * ON_HOLD    - temporarily paused
     * COMPLETED  - all work finished
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PLANNING;

    /**
     * The Admin or Manager who created this project.
     * FetchType.LAZY means Hibernate does NOT load the full User object
     * automatically every time a Project is fetched — only when
     * .getCreatedBy() is actually called. This avoids unnecessary
     * database queries and keeps API responses lean.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Stored as a plain integer column 0-100. This value is calculated
     * by a MySQL stored PROCEDURE (sp_calculate_project_progress) rather
     * than computed in Java — this satisfies the brief's requirement to
     * use PL/SQL-style procedures for "complex DB operations".
     */
    @Column(name = "progress_percent")
    private Integer progressPercent = 0;

    public enum Status {
        PLANNING,
        ACTIVE,
        ON_HOLD,
        COMPLETED
    }
}
