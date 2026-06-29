package com.pms.repository;

import com.pms.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
           SELECT t FROM Task t
           LEFT JOIN FETCH t.project
           LEFT JOIN FETCH t.assignedTo
           WHERE t.project.id = :projectId
           ORDER BY t.createdAt DESC
           """)
    List<Task> findByProjectIdWithDetails(@Param("projectId") Long projectId);

    @Query("""
           SELECT t FROM Task t
           LEFT JOIN FETCH t.project
           LEFT JOIN FETCH t.assignedTo
           WHERE t.id = :id
           """)
    Optional<Task> findByIdWithDetails(@Param("id") Long id);

    @Query("""
           SELECT t FROM Task t
           LEFT JOIN FETCH t.project
           LEFT JOIN FETCH t.assignedTo
           WHERE t.assignedTo.id = :userId
           ORDER BY t.dueDate ASC
           """)
    List<Task> findByAssignedToIdWithDetails(@Param("userId") Long userId);

    @Query("""
           SELECT t FROM Task t
           LEFT JOIN FETCH t.project
           LEFT JOIN FETCH t.assignedTo
           ORDER BY t.createdAt DESC
           """)
    List<Task> findAllWithDetails();

    /**
     * Cross-project "All Tasks" view with full filtering, now including
     * an explicit "overdue" boolean filter alongside status/priority/
     * project/assignee/search. Passing overdue=true restricts results
     * to tasks whose due_date is in the past AND whose status is not
     * DONE — the exact same business rule TaskResponse.overdue already
     * computes per-row, now also usable as a server-side WHERE filter
     * so the dashboard's "Overdue" stat card can link directly to a
     * correctly pre-filtered, paginated view instead of relying on the
     * client to filter an already-paginated page (which would silently
     * under-count overdue tasks sitting on pages 2+).
     *
     * The (:overdue IS NULL OR ...) pattern means: omit the param
     * entirely to not filter on it at all; pass true to restrict to
     * overdue tasks only. We don't bother supporting overdue=false as
     * a meaningful filter since "not overdue" isn't a UI concept here.
     */
    @Query("""
           SELECT t FROM Task t
           LEFT JOIN FETCH t.project
           LEFT JOIN FETCH t.assignedTo
           WHERE (:status IS NULL OR t.status = :status)
             AND (:priority IS NULL OR t.priority = :priority)
             AND (:projectId IS NULL OR t.project.id = :projectId)
             AND (:assignedToUserId IS NULL OR t.assignedTo.id = :assignedToUserId)
             AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:overdue IS NULL OR :overdue = false OR
                  (t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status <> 'DONE'))
           """)
    Page<Task> searchTasks(
            @Param("status") Task.Status status,
            @Param("priority") Task.Priority priority,
            @Param("projectId") Long projectId,
            @Param("assignedToUserId") Long assignedToUserId,
            @Param("search") String search,
            @Param("overdue") Boolean overdue,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    /** Same filtering, scoped to a single Member's own assigned tasks. */
    @Query("""
           SELECT t FROM Task t
           LEFT JOIN FETCH t.project
           LEFT JOIN FETCH t.assignedTo
           WHERE t.assignedTo.id = :userId
             AND (:status IS NULL OR t.status = :status)
             AND (:priority IS NULL OR t.priority = :priority)
             AND (:projectId IS NULL OR t.project.id = :projectId)
             AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:overdue IS NULL OR :overdue = false OR
                  (t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status <> 'DONE'))
           """)
    Page<Task> searchMyTasks(
            @Param("userId") Long userId,
            @Param("status") Task.Status status,
            @Param("priority") Task.Priority priority,
            @Param("projectId") Long projectId,
            @Param("search") String search,
            @Param("overdue") Boolean overdue,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, Task.Status status);

    long countByStatus(Task.Status status);

    long countByAssignedToIdAndStatus(Long userId, Task.Status status);

    long countByAssignedToIdAndStatusNotAndDueDateBefore(
            Long userId, Task.Status status, LocalDate date);

    long countByDueDateBeforeAndStatusNot(LocalDate date, Task.Status status);
}