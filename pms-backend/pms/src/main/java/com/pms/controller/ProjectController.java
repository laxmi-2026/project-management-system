package com.pms.controller;

import com.pms.dto.ProjectRequest;
import com.pms.dto.ProjectResponse;
import com.pms.service.ProjectService;
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
@RequestMapping("/api/projects")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project Management", description = "APIs for creating and managing projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Get all projects visible to the logged-in user (role-aware)")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects(Principal principal) {
        log.info("GET /api/projects | requestedBy={}", principal.getName());
        return ResponseEntity.ok(projectService.getAllProjectsForUser(principal.getName()));
    }

    @Operation(summary = "Get a single project by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id, Principal principal) {
        log.info("GET /api/projects/{} | requestedBy={}", id, principal.getName());
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @Operation(summary = "Create a new project — Admin or Manager only")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request,
                                                         Principal principal) {
        log.info("POST /api/projects | name='{}' requestedBy={}", request.getName(), principal.getName());
        ProjectResponse created = projectService.createProject(request, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Update a project — Admin or Manager only")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id,
                                                         @Valid @RequestBody ProjectRequest request,
                                                         Principal principal) {
        log.info("PUT /api/projects/{} | newName='{}' requestedBy={}", id, request.getName(), principal.getName());
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @Operation(summary = "Delete a project — Admin only, blocked if incomplete tasks exist")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProject(@PathVariable Long id, Principal principal) {
        log.info("DELETE /api/projects/{} | requestedBy={}", id, principal.getName());
        projectService.deleteProject(id);
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }
}
