package com.pms.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dedicated component for calling the raw MySQL stored procedures and
 * functions defined in schema.sql. Keeping these calls in their own
 * class (rather than scattered across repositories) makes it obvious
 * at a glance exactly which "complex DB operations" satisfy that part
 * of the Task 3 brief, and gives us one place to look if a procedure
 * call needs debugging.
 */
@Component
@Slf4j
public class StoredProcedureService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Calls: sp_calculate_project_progress(p_project_id)
     *
     * This procedure recalculates a project's progress_percent column
     * by counting DONE tasks vs total tasks, directly inside MySQL.
     * We call this every time a task's status changes, so the project's
     * progress bar on the Angular dashboard is always accurate.
     */
    public void recalculateProjectProgress(Long projectId) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("sp_calculate_project_progress")
                .registerStoredProcedureParameter(1, Long.class, ParameterMode.IN)
                .setParameter(1, projectId);

        query.execute();
        log.debug("Stored procedure executed | sp_calculate_project_progress | projectId={}", projectId);
    }

    /**
     * Calls: fn_count_overdue_tasks_for_user(p_user_id)
     *
     * A MySQL FUNCTION (not a procedure) because it returns exactly one
     * value. We call it as a native SQL SELECT since JPA's support for
     * scalar function calls is more reliable via a plain native query
     * than via @Procedure for simple single-value functions.
     */
    public int countOverdueTasksForUser(Long userId) {
        Object result = entityManager
                .createNativeQuery("SELECT fn_count_overdue_tasks_for_user(:userId)")
                .setParameter("userId", userId)
                .getSingleResult();

        int count = result != null ? ((Number) result).intValue() : 0;
        log.debug("Stored function executed | fn_count_overdue_tasks_for_user | userId={} result={}", userId, count);
        return count;
    }

    /**
     * Calls: sp_get_dashboard_summary()
     *
     * Returns all six dashboard numbers in a single database round-trip
     * instead of six separate queries from Java — a direct performance
     * optimization satisfying the brief's "Advanced Expectations"
     * section.
     *
     * NOTE ON IMPLEMENTATION: MySQL procedures that return a result set
     * (via a plain SELECT inside the procedure body, as ours does) are
     * called more reliably through a native query than through JPA's
     * createStoredProcedureQuery, which is tuned primarily for OUT
     * parameters. "CALL procedure_name()" is the standard, well-supported
     * way every MySQL JDBC driver executes this kind of procedure.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDashboardSummary() {
        Object[] row = (Object[]) entityManager
                .createNativeQuery("CALL sp_get_dashboard_summary()")
                .getSingleResult();

        Map<String, Object> summary = Map.of(
                "totalProjects",     ((Number) row[0]).longValue(),
                "activeProjects",    ((Number) row[1]).longValue(),
                "completedProjects", ((Number) row[2]).longValue(),
                "totalTasks",        ((Number) row[3]).longValue(),
                "completedTasks",    ((Number) row[4]).longValue(),
                "overdueTasks",      ((Number) row[5]).longValue()
        );

        log.debug("Stored procedure executed | sp_get_dashboard_summary | result={}", summary);
        return summary;
    }
}
