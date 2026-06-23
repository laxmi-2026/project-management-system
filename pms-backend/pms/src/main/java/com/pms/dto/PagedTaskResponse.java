package com.pms.dto;

import java.util.List;

/**
 * Wraps a page of tasks with the pagination metadata the Angular
 * frontend needs to render "Page 2 of 5" controls and know whether
 * Next/Previous buttons should be enabled.
 */
public record PagedTaskResponse(
        List<TaskResponse> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {
}