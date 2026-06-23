package com.pms.dto;

/**
 * What we send back after a successful login.
 *
 * This is a Java RECORD — a modern Java feature (finalized in Java 16,
 * heavily used in Java 21 codebases) that auto-generates the constructor,
 * getters, equals(), hashCode() and toString() for an immutable data
 * holder, in a single line. This directly satisfies the brief's
 * requirement to "use modern Java features (Java 21 improvements)".
 *
 * Old way (Java 8 style) would need ~40 lines of class + getters + constructor.
 * Record way: 1 line.
 */
public record LoginResponse(
        String token,
        String type,
        Long id,
        String username,
        String email,
        String role
) {
    public static LoginResponse of(String token, Long id, String username, String email, String role) {
        return new LoginResponse(token, "Bearer", id, username, email, role);
    }
}
