package com.pms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a registered user of the system.
 *
 * A user can have one of three roles:
 *   ROLE_ADMIN   - full access to everything
 *   ROLE_MANAGER - can create/manage projects and tasks (acts like Admin for their own projects)
 *   ROLE_MEMBER  - the "User" role from the brief; can only view/update their own assigned tasks
 *
 * Lombok's @Data auto-generates getters, setters, toString, equals, hashCode —
 * this avoids writing ~100 lines of boilerplate by hand.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.ROLE_MEMBER;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Defining the role as an enum (rather than a plain String) means the
     * compiler stops us from ever typing "ROLE_ADMN" by mistake — typos
     * become compile errors instead of silent runtime bugs.
     */
    public enum Role {
        ROLE_ADMIN,
        ROLE_MANAGER,
        ROLE_MEMBER
    }
}
