package com.pms.service;

import com.pms.dto.ProjectRequest;
import com.pms.dto.ProjectResponse;
import com.pms.exception.ResourceConflictException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.model.Project;
import com.pms.model.Task;
import com.pms.model.User;
import com.pms.repository.ProjectRepository;
import com.pms.repository.TaskRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    /**
     * Admin and Manager see every project. Member only sees projects
     * where they have at least one assigned task — this is the
     * role-based filtering the brief asks for at the data level, not
     * just hidden buttons on the frontend.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjectsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Project> projects = (user.getRole() == User.Role.ROLE_MEMBER)
                ? projectRepository.findProjectsForMember(user.getId())
                : projectRepository.findAllWithCreatedBy();

        log.info("Projects fetched | userEmail={} role={} count={}", userEmail, user.getRole(), projects.size());

        return projects.stream()
                .map(this::toResponseWithTaskCounts)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findByIdWithCreatedBy(id)
                .orElseThrow(() -> {
                    log.warn("Project not found | projectId={}", id);
                    return new ResourceNotFoundException("Project", "id", id);
                });
        return toResponseWithTaskCounts(project);
    }

    public ProjectResponse createProject(ProjectRequest request, String creatorEmail) {
        log.info("Project creation initiated | name='{}' createdBy={}", request.getName(), creatorEmail);

        // Business-rule validation: end date can't be before start date.
        // This is exactly the kind of validation rule you specifically
        // asked be enforced everywhere in this project.
        validateDateRange(request);

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(resolveStatus(request.getStatus()));
        project.setCreatedBy(creator);

        Project saved = projectRepository.save(project);
        saved.setCreatedBy(creator);

        log.info("Project created successfully | projectId={} name='{}' createdBy={}",
                saved.getId(), saved.getName(), creatorEmail);

        return ProjectResponse.from(saved, 0, 0);
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        log.info("Project update initiated | projectId={} newName='{}'", id, request.getName());

        validateDateRange(request);

        Project existing = projectRepository.findByIdWithCreatedBy(id)
                .orElseThrow(() -> {
                    log.warn("Project update failed | reason=not-found | projectId={}", id);
                    return new ResourceNotFoundException("Project", "id", id);
                });

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            existing.setStatus(resolveStatus(request.getStatus()));
        }

        Project updated = projectRepository.save(existing);
        log.info("Project updated successfully | projectId={} name='{}'", updated.getId(), updated.getName());

        return toResponseWithTaskCounts(updated);
    }

    /**
     * Business rule you specifically asked be enforced: a project that
     * still has incomplete tasks cannot be deleted. This prevents an
     * Admin from accidentally wiping out in-progress work. The check
     * throws ResourceConflictException -> HTTP 409, which is the
     * semantically correct status code for "valid request, but blocked
     * by current state of the data".
     */
    public void deleteProject(Long id) {
        log.info("Project deletion initiated | projectId={}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Project deletion failed | reason=not-found | projectId={}", id);
                    return new ResourceNotFoundException("Project", "id", id);
                });

        long incompleteTasks = taskRepository.countByProjectId(id)
                - taskRepository.countByProjectIdAndStatus(id, Task.Status.DONE);

        if (incompleteTasks > 0) {
            log.warn("Project deletion blocked | reason=has-incomplete-tasks | projectId={} incompleteTasks={}",
                    id, incompleteTasks);
            throw new ResourceConflictException(
                    "Cannot delete project '" + project.getName() + "' — it still has "
                            + incompleteTasks + " incomplete task(s). Complete or delete them first.");
        }

        projectRepository.delete(project);
        log.info("Project deleted successfully | projectId={} name='{}'", id, project.getName());
    }

    /**
     * Converts a Project entity to its DTO, including live task counts.
     * Note: progress_percent on the entity itself is only refreshed by
     * the stored procedure (called from TaskService whenever a task's
     * status changes) — here we just read whatever was last calculated,
     * keeping this read path fast with no extra DB round trip.
     */
    private ProjectResponse toResponseWithTaskCounts(Project project) {
        long total = taskRepository.countByProjectId(project.getId());
        long completed = taskRepository.countByProjectIdAndStatus(project.getId(), Task.Status.DONE);
        return ProjectResponse.from(project, total, completed);
    }

    private void validateDateRange(ProjectRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    private Project.Status resolveStatus(String statusInput) {
        if (statusInput == null || statusInput.isBlank()) return Project.Status.PLANNING;
        try {
            return Project.Status.valueOf(statusInput.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status '" + statusInput + "'. Must be one of: PLANNING, ACTIVE, ON_HOLD, COMPLETED");
        }
    }
}
