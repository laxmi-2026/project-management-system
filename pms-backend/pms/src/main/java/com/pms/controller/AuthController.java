package com.pms.controller;

import com.pms.dto.LoginRequest;
import com.pms.dto.LoginResponse;
import com.pms.dto.RegisterRequest;
import com.pms.model.User;
import com.pms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Register and Login APIs")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register | email={}", request.getEmail());

        User user = authService.register(request);

        log.info("Registration completed | userId={} email={} role={}",
                user.getId(), user.getEmail(), user.getRole());

        return new ResponseEntity<>(
                Map.of("message", "User registered successfully", "email", user.getEmail()),
                HttpStatus.CREATED);
    }

    @Operation(summary = "Login and receive JWT token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login | email={}", request.getEmail());

        LoginResponse response = authService.login(request);

        log.info("Login response sent | email={} role={}", response.email(), response.role());

        return ResponseEntity.ok(response);
    }
}
