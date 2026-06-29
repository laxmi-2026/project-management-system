package com.pms.controller;

import com.pms.dto.PagedTaskResponse;
import com.pms.dto.TaskRequest;
import com.pms.dto.TaskResponse;
import com.pms.dto.TaskStatusUpdateRequest;
import com.pms.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Management", description = "APIs for creating, assigning, and tracking tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Search/filter tasks across all projects with pagination, including an overdue-only filter")
    @GetMapping
    public ResponseEntity<PagedTaskResponse> searchTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assignedToUserId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal
    ) {
        log.info("GET /api/tasks | status={} priority={} projectId={} overdue={} page={} requestedBy={}",
                status, priority, projectId, overdue, page, principal.getName());

        PagedTaskResponse result = taskService.searchTasks(
                principal.getName(), status, priority, projectId, assignedToUserId,
                search, overdue, page, size, sortBy, sortDir
        );

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get all tasks for a specific project — feeds the Kanban board")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable Long projectId,
                                                                Principal principal) {
        log.info("GET /api/tasks/project/{} | requestedBy={}", projectId, principal.getName());
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @Operation(summary = "Get only the tasks assigned to the logged-in user")
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(Principal principal) {
        log.info("GET /api/tasks/my-tasks | requestedBy={}", principal.getName());
        return ResponseEntity.ok(taskService.getMyTasks(principal.getName()));
    }

    @Operation(summary = "Get a single task by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id, Principal principal) {
        log.info("GET /api/tasks/{} | requestedBy={}", id, principal.getName());
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @Operation(summary = "Create a new task — Admin or Manager only")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request,
                                                   Principal principal) {
        log.info("POST /api/tasks | title='{}' projectId={} requestedBy={}",
                request.getTitle(), request.getProjectId(), principal.getName());
        TaskResponse created = taskService.createTask(request, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Update full task details — Admin or Manager only")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id,
                                                   @Valid @RequestBody TaskRequest request,
                                                   Principal principal) {
        log.info("PUT /api/tasks/{} | newTitle='{}' requestedBy={}", id, request.getTitle(), principal.getName());
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @Operation(summary = "Update only the status of a task — any authenticated user (Kanban drag-and-drop)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(@PathVariable Long id,
                                                         @Valid @RequestBody TaskStatusUpdateRequest request,
                                                         Principal principal) {
        log.info("PATCH /api/tasks/{}/status | newStatus={} requestedBy={}",
                id, request.getStatus(), principal.getName());
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request));
    }

    @Operation(summary = "Delete a task — Admin or Manager only")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable Long id, Principal principal) {
        log.info("DELETE /api/tasks/{} | requestedBy={}", id, principal.getName());
        taskService.deleteTask(id);
        return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
    }
}