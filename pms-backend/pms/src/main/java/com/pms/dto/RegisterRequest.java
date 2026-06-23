package com.pms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * What the frontend sends when a new user registers.
 *
 * PRODUCTION VALIDATION RULES applied here:
 *   - username: 3-50 chars, alphanumeric + underscore only (prevents
 *     injection-style usernames, keeps URLs/display clean)
 *   - password: minimum 8 characters (industry baseline — NIST SP
 *     800-63B recommends 8+ as the practical minimum for user-chosen
 *     passwords, since overly complex composition rules tend to push
 *     users toward predictable patterns instead of real entropy)
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * Optional — if left blank, the service defaults this to ROLE_MEMBER.
     * Frontend sends "ADMIN", "MANAGER", or leaves it empty for Member.
     */
    private String role;
}