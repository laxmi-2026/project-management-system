package com.pms.service;

import com.pms.dto.PagedTaskResponse;
import com.pms.dto.TaskRequest;
import com.pms.dto.TaskResponse;
import com.pms.dto.TaskStatusUpdateRequest;
import com.pms.exception.ResourceNotFoundException;
import com.pms.model.Project;
import com.pms.model.Task;
import com.pms.model.User;
import com.pms.repository.ProjectRepository;
import com.pms.repository.StoredProcedureService;
import com.pms.repository.TaskRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final StoredProcedureService storedProcedureService;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId) {
        log.debug("Fetching tasks | projectId={}", projectId);
        List<TaskResponse> tasks = taskRepository.findByProjectIdWithDetails(projectId)
                .stream()
                .map(TaskResponse::from)
                .toList();
        log.info("Tasks fetched for project | projectId={} count={}", projectId, tasks.size());
        return tasks;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<TaskResponse> tasks = taskRepository.findByAssignedToIdWithDetails(user.getId())
                .stream()
                .map(TaskResponse::from)
                .toList();

        log.info("My-tasks fetched | userEmail={} count={}", userEmail, tasks.size());
        return tasks;
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> {
                    log.warn("Task not found | taskId={}", id);
                    return new ResourceNotFoundException("Task", "id", id);
                });
        return TaskResponse.from(task);
    }

    /**
     * Powers the cross-project "All Tasks" page, including the
     * dashboard's "Overdue" stat card, which now passes overdue=true
     * here instead of relying on any client-side post-filtering. This
     * keeps the overdue count and the overdue list mathematically
     * consistent with each other and with pagination — filtering
     * happens in the SQL WHERE clause, before pagination is applied,
     * not after a page of results has already been fetched.
     */
    @Transactional(readOnly = true)
    public PagedTaskResponse searchTasks(
            String userEmail,
            String statusStr,
            String priorityStr,
            Long projectId,
            Long assignedToUserId,
            String search,
            Boolean overdue,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Task.Status status = parseStatus(statusStr);
        Task.Priority priority = parsePriority(priorityStr);
        LocalDate today = LocalDate.now();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<Task> result = (user.getRole() == User.Role.ROLE_MEMBER)
                ? taskRepository.searchMyTasks(user.getId(), status, priority, projectId, search, overdue, today, pageable)
                : taskRepository.searchTasks(status, priority, projectId, assignedToUserId, search, overdue, today, pageable);

        log.info("Task search | userEmail={} role={} status={} priority={} projectId={} overdue={} page={} totalResults={}",
                userEmail, user.getRole(), statusStr, priorityStr, projectId, overdue, page, result.getTotalElements());

        return new PagedTaskResponse(
                result.getContent().stream().map(TaskResponse::from).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                page,
                size
        );
    }

    public TaskResponse createTask(TaskRequest request, String creatorEmail) {
        log.info("Task creation initiated | title='{}' projectId={} createdBy={}",
                request.getTitle(), request.getProjectId(), creatorEmail);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));

        if (request.getDueDate() != null && project.getStartDate() != null
                && request.getDueDate().isBefore(project.getStartDate())) {
            throw new IllegalArgumentException(
                    "Task due date cannot be before the project's start date (" + project.getStartDate() + ")");
        }

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setPriority(resolvePriority(request.getPriority()));
        task.setStatus(resolveStatus(request.getStatus()));
        task.setProject(project);
        task.setCreatedBy(creator);

        if (request.getAssignedToUserId() != null) {
            User assignee = userRepository.findById(request.getAssignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedToUserId()));
            task.setAssignedTo(assignee);
        }

        Task saved = taskRepository.save(task);
        storedProcedureService.recalculateProjectProgress(project.getId());

        log.info("Task created successfully | taskId={} title='{}' projectId={} assignedTo={}",
                saved.getId(), saved.getTitle(), project.getId(),
                saved.getAssignedTo() != null ? saved.getAssignedTo().getUsername() : "unassigned");

        return TaskResponse.from(taskRepository.findByIdWithDetails(saved.getId()).orElseThrow());
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        log.info("Task update initiated | taskId={} newTitle='{}'", id, request.getTitle());

        Task existing = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> {
                    log.warn("Task update failed | reason=not-found | taskId={}", id);
                    return new ResourceNotFoundException("Task", "id", id);
                });

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setDueDate(request.getDueDate());
        if (request.getPriority() != null) existing.setPriority(resolvePriority(request.getPriority()));
        if (request.getStatus() != null)   existing.setStatus(resolveStatus(request.getStatus()));
        existing.setUpdatedAt(LocalDateTime.now());

        if (request.getAssignedToUserId() != null) {
            User assignee = userRepository.findById(request.getAssignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedToUserId()));
            existing.setAssignedTo(assignee);
        }

        Task updated = taskRepository.save(existing);
        storedProcedureService.recalculateProjectProgress(updated.getProject().getId());

        log.info("Task updated successfully | taskId={} status={}", updated.getId(), updated.getStatus());
        return TaskResponse.from(updated);
    }

    public TaskResponse updateTaskStatus(Long id, TaskStatusUpdateRequest request) {
        log.info("Task status update initiated | taskId={} newStatus={}", id, request.getStatus());

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        task.setStatus(resolveStatus(request.getStatus()));
        task.setUpdatedAt(LocalDateTime.now());

        Task updated = taskRepository.save(task);
        storedProcedureService.recalculateProjectProgress(updated.getProject().getId());

        log.info("Task status updated successfully | taskId={} status={} projectId={}",
                updated.getId(), updated.getStatus(), updated.getProject().getId());

        return TaskResponse.from(updated);
    }

    public void deleteTask(Long id) {
        log.info("Task deletion initiated | taskId={}", id);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Task deletion failed | reason=not-found | taskId={}", id);
                    return new ResourceNotFoundException("Task", "id", id);
                });

        Long projectId = task.getProject().getId();
        taskRepository.delete(task);
        storedProcedureService.recalculateProjectProgress(projectId);

        log.info("Task deleted successfully | taskId={} title='{}'", id, task.getTitle());
    }

    private Task.Priority resolvePriority(String input) {
        if (input == null || input.isBlank()) return Task.Priority.MEDIUM;
        try {
            return Task.Priority.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority '" + input + "'. Must be LOW, MEDIUM, or HIGH");
        }
    }

    private Task.Status resolveStatus(String input) {
        if (input == null || input.isBlank()) return Task.Status.TODO;
        try {
            return Task.Status.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + input + "'. Must be TODO, IN_PROGRESS, or DONE");
        }
    }

    private Task.Status parseStatus(String input) {
        if (input == null || input.isBlank()) return null;
        return Task.Status.valueOf(input.trim().toUpperCase());
    }

    private Task.Priority parsePriority(String input) {
        if (input == null || input.isBlank()) return null;
        return Task.Priority.valueOf(input.trim().toUpperCase());
    }
}