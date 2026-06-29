package com.pms.exception;

/**
 * Thrown when a request is technically valid but violates a business
 * rule — for example, trying to delete a Project that still has
 * incomplete Tasks. Maps to HTTP 409 Conflict, which correctly tells
 * the frontend "your request was understood but can't be done right now"
 * as opposed to 400 (malformed request) or 404 (doesn't exist).
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
