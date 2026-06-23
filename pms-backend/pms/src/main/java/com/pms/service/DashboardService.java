package com.pms.service;

import com.pms.dto.DashboardStats;
import com.pms.dto.TaskResponse;
import com.pms.exception.ResourceNotFoundException;
import com.pms.model.Task;
import com.pms.model.User;
import com.pms.repository.ProjectRepository;
import com.pms.repository.StoredProcedureService;
import com.pms.repository.TaskRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final StoredProcedureService storedProcedureService;

    /**
     * Returns different numbers depending on who's asking:
     *   - ADMIN / MANAGER: company-wide totals via sp_get_dashboard_summary()
     *   - MEMBER: only their own assigned task counts + their personal
     *             overdue count via fn_count_overdue_tasks_for_user()
     */
    @Transactional(readOnly = true)
    public DashboardStats getDashboard(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Dashboard requested | userEmail={} role={}", userEmail, user.getRole());

        if (user.getRole() == User.Role.ROLE_MEMBER) {
            return buildMemberDashboard(user);
        }
        return buildAdminDashboard();
    }

    /**
     * Calls sp_get_dashboard_summary() — a single round trip to MySQL
     * that returns all six aggregate numbers at once, rather than six
     * separate JPA count queries from Java.
     */
    private DashboardStats buildAdminDashboard() {
        Map<String, Object> summary = storedProcedureService.getDashboardSummary();

        Map<String, Long> tasksByStatus = Map.of(
                "TODO",        taskRepository.countByStatus(Task.Status.TODO),
                "IN_PROGRESS", taskRepository.countByStatus(Task.Status.IN_PROGRESS),
                "DONE",        taskRepository.countByStatus(Task.Status.DONE)
        );

        List<TaskResponse> recent = taskRepository.findAllWithDetails()
                .stream()
                .limit(5)
                .map(TaskResponse::from)
                .toList();

        log.info("Admin dashboard built | totalProjects={} totalTasks={} overdueTasks={}",
                summary.get("totalProjects"), summary.get("totalTasks"), summary.get("overdueTasks"));

        return new DashboardStats(
                (Long) summary.get("totalProjects"),
                (Long) summary.get("activeProjects"),
                (Long) summary.get("completedProjects"),
                (Long) summary.get("totalTasks"),
                (Long) summary.get("completedTasks"),
                (Long) summary.get("overdueTasks"),
                tasksByStatus,
                recent
        );
    }

    /**
     * Calls fn_count_overdue_tasks_for_user() — a MySQL FUNCTION (not a
     * procedure) since it returns exactly one scalar value, used inline
     * for this one Member's overdue count.
     */
    private DashboardStats buildMemberDashboard(User member) {
        long totalTasks     = taskRepository.findByAssignedToIdWithDetails(member.getId()).size();
        long completedTasks = taskRepository.countByAssignedToIdAndStatus(member.getId(), Task.Status.DONE);
        int overdueTasks    = storedProcedureService.countOverdueTasksForUser(member.getId());

        long memberProjects = projectRepository.findProjectsForMember(member.getId()).size();

        Map<String, Long> tasksByStatus = Map.of(
                "TODO",        taskRepository.countByAssignedToIdAndStatus(member.getId(), Task.Status.TODO),
                "IN_PROGRESS", taskRepository.countByAssignedToIdAndStatus(member.getId(), Task.Status.IN_PROGRESS),
                "DONE",        completedTasks
        );

        List<TaskResponse> myRecent = taskRepository.findByAssignedToIdWithDetails(member.getId())
                .stream()
                .limit(5)
                .map(TaskResponse::from)
                .toList();

        log.info("Member dashboard built | userId={} totalTasks={} overdueTasks={}",
                member.getId(), totalTasks, overdueTasks);

        return new DashboardStats(
                memberProjects,
                memberProjects, // for a Member, "active" == projects they're part of
                0,
                totalTasks,
                completedTasks,
                overdueTasks,
                tasksByStatus,
                myRecent
        );
    }
}
