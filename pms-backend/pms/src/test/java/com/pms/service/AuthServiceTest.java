package com.pms.service;

import com.pms.dto.LoginRequest;
import com.pms.dto.LoginResponse;
import com.pms.dto.RegisterRequest;
import com.pms.model.User;
import com.pms.repository.UserRepository;
import com.pms.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@pms.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@pms.com");
        loginRequest.setPassword("password123");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@pms.com");
        mockUser.setPassword("$2a$10$hashedPassword");
        mockUser.setRole(User.Role.ROLE_MEMBER);
    }

    @Test
    @DisplayName("Register - Success with ROLE_MEMBER by default")
    void testRegister_Success_DefaultMemberRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(User.Role.ROLE_MEMBER, result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Duplicate email throws RuntimeException")
    void testRegister_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("test@pms.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("already registered"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register - Duplicate username throws RuntimeException")
    void testRegister_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("already taken"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register - ROLE_ADMIN assigned when role field is ADMIN")
    void testRegister_AdminRole_WhenRoleSetToAdmin() {
        registerRequest.setRole("ADMIN");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");

        User adminUser = new User();
        adminUser.setRole(User.Role.ROLE_ADMIN);
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        User result = authService.register(registerRequest);

        assertEquals(User.Role.ROLE_ADMIN, result.getRole());
    }

    @Test
    @DisplayName("Register - ROLE_MANAGER assigned when role field is MANAGER")
    void testRegister_ManagerRole_WhenRoleSetToManager() {
        registerRequest.setRole("MANAGER");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");

        User managerUser = new User();
        managerUser.setRole(User.Role.ROLE_MANAGER);
        when(userRepository.save(any(User.class))).thenReturn(managerUser);

        User result = authService.register(registerRequest);

        assertEquals(User.Role.ROLE_MANAGER, result.getRole());
    }

    @Test
    @DisplayName("Login - Success returns token and user info")
    void testLogin_Success_ReturnsTokenAndUserInfo() {
        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User(
                        "test@pms.com", "$2a$10$hashedPassword",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER")));

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(springUser);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(jwtUtil.generateToken(any())).thenReturn("mockJwtToken123");
        when(userRepository.findByEmail("test@pms.com")).thenReturn(Optional.of(mockUser));

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mockJwtToken123", response.token());
        assertEquals("test@pms.com", response.email());
        assertEquals("ROLE_MEMBER", response.role());
    }

    @Test
    @DisplayName("Login - Invalid credentials throws BadCredentialsException")
    void testLogin_InvalidCredentials_ThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(jwtUtil, never()).generateToken(any());
    }
}
