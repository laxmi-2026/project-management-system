package com.pms.exception;

/**
 * Thrown whenever code looks up something by ID and it doesn't exist —
 * a missing Project, Task, or User. GlobalExceptionHandler catches this
 * and converts it into a clean 404 JSON response automatically.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
