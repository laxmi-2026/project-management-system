package com.pms.controller;

import com.pms.dto.DashboardStats;
import com.pms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Role-aware reporting and statistics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard statistics — content differs by role (Admin/Manager see company-wide stats, Member sees only their own)")
    @GetMapping
    public ResponseEntity<DashboardStats> getDashboard(Principal principal) {
        log.info("GET /api/dashboard | requestedBy={}", principal.getName());
        return ResponseEntity.ok(dashboardService.getDashboard(principal.getName()));
    }
}
