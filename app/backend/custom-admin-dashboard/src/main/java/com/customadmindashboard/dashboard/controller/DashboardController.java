package com.customadmindashboard.dashboard.controller;

import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.response.ApiResponse;
import com.customadmindashboard.dashboard.dto.*;
import com.customadmindashboard.dashboard.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PostMapping("/views")
    public ResponseEntity<ApiResponse<DashboardViewResponse>> createView(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateDashboardViewRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        DashboardViewResponse response = dashboardService.createView(projectId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(response, "Dashboard view created successfully"));
    }

    @GetMapping("/views")
    public ResponseEntity<ApiResponse<List<DashboardViewResponse>>> getViews(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Tenant tenant) {
        List<DashboardViewResponse> views = dashboardService.getViews(projectId, tenant);
        return ResponseEntity.ok(ApiResponse.success(views));
    }

    @PutMapping("/views/{viewId}/columns")
    public ResponseEntity<ApiResponse<DashboardColumnResponse>> updateColumn(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @Valid @RequestBody UpdateDashboardColumnRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        DashboardColumnResponse response = dashboardService.updateColumn(viewId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(response, "Column updated successfully"));
    }
}