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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project mockProject;
    private User mockAdmin;
    private User mockMember;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        mockAdmin = new User();
        mockAdmin.setId(1L);
        mockAdmin.setUsername("admin");
        mockAdmin.setEmail("admin@pms.com");
        mockAdmin.setRole(User.Role.ROLE_ADMIN);

        mockMember = new User();
        mockMember.setId(2L);
        mockMember.setUsername("member");
        mockMember.setEmail("member@pms.com");
        mockMember.setRole(User.Role.ROLE_MEMBER);

        mockProject = new Project();
        mockProject.setId(1L);
        mockProject.setName("Website Redesign");
        mockProject.setStatus(Project.Status.ACTIVE);
        mockProject.setCreatedBy(mockAdmin);
        mockProject.setStartDate(LocalDate.of(2026, 1, 1));
        mockProject.setEndDate(LocalDate.of(2026, 6, 1));

        projectRequest = new ProjectRequest();
        projectRequest.setName("Website Redesign");
        projectRequest.setStartDate(LocalDate.of(2026, 1, 1));
        projectRequest.setEndDate(LocalDate.of(2026, 6, 1));
        projectRequest.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Get all projects - Admin sees all projects via findAllWithCreatedBy")
    void testGetAllProjects_Admin_SeesAllProjects() {
        when(userRepository.findByEmail("admin@pms.com")).thenReturn(Optional.of(mockAdmin));
        when(projectRepository.findAllWithCreatedBy()).thenReturn(Arrays.asList(mockProject));
        when(taskRepository.countByProjectId(1L)).thenReturn(5L);
        when(taskRepository.countByProjectIdAndStatus(1L, Task.Status.DONE)).thenReturn(2L);

        List<ProjectResponse> result = projectService.getAllProjectsForUser("admin@pms.com");

        assertEquals(1, result.size());
        assertEquals("Website Redesign", result.get(0).name());
        verify(projectRepository, times(1)).findAllWithCreatedBy();
        verify(projectRepository, never()).findProjectsForMember(any());
    }

    @Test
    @DisplayName("Get all projects - Member sees only their assigned-task projects")
    void testGetAllProjects_Member_SeesOnlyOwnProjects() {
        when(userRepository.findByEmail("member@pms.com")).thenReturn(Optional.of(mockMember));
        when(projectRepository.findProjectsForMember(2L)).thenReturn(Arrays.asList(mockProject));
        when(taskRepository.countByProjectId(anyLong())).thenReturn(3L);
        when(taskRepository.countByProjectIdAndStatus(anyLong(), any())).thenReturn(1L);

        List<ProjectResponse> result = projectService.getAllProjectsForUser("member@pms.com");

        assertEquals(1, result.size());
        verify(projectRepository, times(1)).findProjectsForMember(2L);
        verify(projectRepository, never()).findAllWithCreatedBy();
    }

    @Test
    @DisplayName("Create project - Success returns ProjectResponse")
    void testCreateProject_Success() {
        when(userRepository.findByEmail("admin@pms.com")).thenReturn(Optional.of(mockAdmin));
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        ProjectResponse result = projectService.createProject(projectRequest, "admin@pms.com");

        assertNotNull(result);
        assertEquals("Website Redesign", result.name());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("Create project - End date before start date throws IllegalArgumentException")
    void testCreateProject_EndDateBeforeStartDate_ThrowsException() {
        projectRequest.setStartDate(LocalDate.of(2026, 6, 1));
        projectRequest.setEndDate(LocalDate.of(2026, 1, 1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> projectService.createProject(projectRequest, "admin@pms.com"));

        assertTrue(exception.getMessage().contains("End date cannot be before start date"));
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create project - Creator not found throws ResourceNotFoundException")
    void testCreateProject_CreatorNotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@pms.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> projectService.createProject(projectRequest, "unknown@pms.com"));

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update project - Success returns updated ProjectResponse")
    void testUpdateProject_Success() {
        projectRequest.setName("Website Redesign V2");
        when(projectRepository.findByIdWithCreatedBy(1L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);
        when(taskRepository.countByProjectId(1L)).thenReturn(5L);
        when(taskRepository.countByProjectIdAndStatus(1L, Task.Status.DONE)).thenReturn(2L);

        ProjectResponse result = projectService.updateProject(1L, projectRequest);

        assertNotNull(result);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("Update project - Not found throws ResourceNotFoundException")
    void testUpdateProject_NotFound_ThrowsException() {
        when(projectRepository.findByIdWithCreatedBy(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> projectService.updateProject(99L, projectRequest));

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete project - Success when all tasks are DONE")
    void testDeleteProject_Success_AllTasksDone() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.countByProjectId(1L)).thenReturn(3L);
        when(taskRepository.countByProjectIdAndStatus(1L, Task.Status.DONE)).thenReturn(3L);

        assertDoesNotThrow(() -> projectService.deleteProject(1L));

        verify(projectRepository, times(1)).delete(mockProject);
    }

    @Test
    @DisplayName("Delete project - Blocked when incomplete tasks exist (ResourceConflictException)")
    void testDeleteProject_Blocked_IncompleteTasksExist() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.countByProjectId(1L)).thenReturn(5L);
        when(taskRepository.countByProjectIdAndStatus(1L, Task.Status.DONE)).thenReturn(2L);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class,
                () -> projectService.deleteProject(1L));

        assertTrue(exception.getMessage().contains("incomplete task"));
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    @DisplayName("Delete project - Not found throws ResourceNotFoundException")
    void testDeleteProject_NotFound_ThrowsException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.deleteProject(99L));
        verify(projectRepository, never()).delete(any(Project.class));
    }
}
