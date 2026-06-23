package com.pms.service;

import com.pms.dto.LoginRequest;
import com.pms.dto.LoginResponse;
import com.pms.dto.RegisterRequest;
import com.pms.model.User;
import com.pms.repository.UserRepository;
import com.pms.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public User register(RegisterRequest request) {
        log.info("User registration initiated | email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed | reason=duplicate-email | email={}", request.getEmail());
            throw new RuntimeException("Email '" + request.getEmail() + "' is already registered");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed | reason=duplicate-username | username={}", request.getUsername());
            throw new RuntimeException("Username '" + request.getUsername() + "' is already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(resolveRole(request.getRole()));

        User saved = userRepository.save(user);
        log.info("User registered successfully | userId={} email={} role={}",
                saved.getId(), saved.getEmail(), saved.getRole());

        return saved;
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt | email={}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Login successful | userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());

        return LoginResponse.of(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }

    /**
     * Accepts "ADMIN", "MANAGER", or anything else (including null/blank,
     * which defaults to MEMBER — the "User" role from the brief). Doing
     * this resolution in one place avoids scattering equalsIgnoreCase
     * checks across multiple methods.
     */
    private User.Role resolveRole(String roleInput) {
        if (roleInput == null) return User.Role.ROLE_MEMBER;
        return switch (roleInput.trim().toUpperCase()) {
            case "ADMIN"   -> User.Role.ROLE_ADMIN;
            case "MANAGER" -> User.Role.ROLE_MANAGER;
            default        -> User.Role.ROLE_MEMBER;
        };
    }
}
