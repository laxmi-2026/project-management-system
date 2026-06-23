package com.pms.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private StoredProcedureService storedProcedureService;

    @InjectMocks
    private TaskService taskService;

    private Project mockProject;
    private User mockManager;
    private User mockMember;
    private Task mockTask;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() {
        mockManager = new User();
        mockManager.setId(1L);
        mockManager.setUsername("manager");
        mockManager.setEmail("manager@pms.com");
        mockManager.setRole(User.Role.ROLE_MANAGER);

        mockMember = new User();
        mockMember.setId(2L);
        mockMember.setUsername("member");
        mockMember.setEmail("member@pms.com");
        mockMember.setRole(User.Role.ROLE_MEMBER);

        mockProject = new Project();
        mockProject.setId(10L);
        mockProject.setName("Website Redesign");
        mockProject.setStartDate(LocalDate.of(2026, 1, 1));

        mockTask = new Task();
        mockTask.setId(100L);
        mockTask.setTitle("Design homepage");
        mockTask.setStatus(Task.Status.TODO);
        mockTask.setPriority(Task.Priority.HIGH);
        mockTask.setProject(mockProject);
        mockTask.setAssignedTo(mockMember);
        mockTask.setCreatedBy(mockManager);
        mockTask.setDueDate(LocalDate.of(2026, 2, 1));

        taskRequest = new TaskRequest();
        taskRequest.setTitle("Design homepage");
        taskRequest.setProjectId(10L);
        taskRequest.setPriority("HIGH");
        taskRequest.setStatus("TODO");
        taskRequest.setDueDate(LocalDate.of(2026, 2, 1));
        taskRequest.setAssignedToUserId(2L);
    }

    @Test
    @DisplayName("Create task - Success calls stored procedure to recalculate progress")
    void testCreateTask_Success_RecalculatesProgress() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(userRepository.findByEmail("manager@pms.com")).thenReturn(Optional.of(mockManager));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockMember));
        when(taskRepository.save(any(Task.class))).thenReturn(mockTask);
        when(taskRepository.findByIdWithDetails(any())).thenReturn(Optional.of(mockTask));

        TaskResponse result = taskService.createTask(taskRequest, "manager@pms.com");

        assertNotNull(result);
        assertEquals("Design homepage", result.title());
        verify(storedProcedureService, times(1)).recalculateProjectProgress(10L);
    }

    @Test
    @DisplayName("Create task - Due date before project start date throws IllegalArgumentException")
    void testCreateTask_DueDateBeforeProjectStart_ThrowsException() {
        taskRequest.setDueDate(LocalDate.of(2025, 12, 1)); // before project start of 2026-01-01
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(taskRequest, "manager@pms.com"));

        assertTrue(exception.getMessage().contains("cannot be before the project's start date"));
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create task - Project not found throws ResourceNotFoundException")
    void testCreateTask_ProjectNotFound_ThrowsException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        taskRequest.setProjectId(99L);

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.createTask(taskRequest, "manager@pms.com"));

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update task status - Success, calls stored procedure")
    void testUpdateTaskStatus_Success_CallsStoredProcedure() {
        TaskStatusUpdateRequest statusRequest = new TaskStatusUpdateRequest();
        statusRequest.setStatus("DONE");

        when(taskRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.updateTaskStatus(100L, statusRequest);

        assertEquals("DONE", result.status());
        verify(storedProcedureService, times(1)).recalculateProjectProgress(10L);
    }

    @Test
    @DisplayName("Update task status - Invalid status throws IllegalArgumentException")
    void testUpdateTaskStatus_InvalidStatus_ThrowsException() {
        TaskStatusUpdateRequest statusRequest = new TaskStatusUpdateRequest();
        statusRequest.setStatus("INVALID_STATUS");

        when(taskRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(mockTask));

        assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTaskStatus(100L, statusRequest));
    }

    @Test
    @DisplayName("Update task status - Task not found throws ResourceNotFoundException")
    void testUpdateTaskStatus_TaskNotFound_ThrowsException() {
        TaskStatusUpdateRequest statusRequest = new TaskStatusUpdateRequest();
        statusRequest.setStatus("DONE");

        when(taskRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTaskStatus(999L, statusRequest));

        verify(storedProcedureService, never()).recalculateProjectProgress(anyLong());
    }

    @Test
    @DisplayName("Get my tasks - Returns only tasks assigned to the user")
    void testGetMyTasks_ReturnsAssignedTasks() {
        when(userRepository.findByEmail("member@pms.com")).thenReturn(Optional.of(mockMember));
        when(taskRepository.findByAssignedToIdWithDetails(2L)).thenReturn(java.util.List.of(mockTask));

        var result = taskService.getMyTasks("member@pms.com");

        assertEquals(1, result.size());
        assertEquals("Design homepage", result.get(0).title());
    }

    @Test
    @DisplayName("Delete task - Success recalculates project progress")
    void testDeleteTask_Success_RecalculatesProgress() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

        assertDoesNotThrow(() -> taskService.deleteTask(100L));

        verify(taskRepository, times(1)).delete(mockTask);
        verify(storedProcedureService, times(1)).recalculateProjectProgress(10L);
    }

    @Test
    @DisplayName("Delete task - Not found throws ResourceNotFoundException")
    void testDeleteTask_NotFound_ThrowsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(999L));
        verify(storedProcedureService, never()).recalculateProjectProgress(anyLong());
    }
}
