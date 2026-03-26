package com.customadmindashboard.dashboard.controller;

import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.response.ApiResponse;
import com.customadmindashboard.dashboard.dto.DashboardSettingsDto;
import com.customadmindashboard.dashboard.service.DashboardSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/settings")
@RequiredArgsConstructor
public class DashboardSettingsController {

    private final DashboardSettingsService dashboardSettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardSettingsDto>> getSettings(
            @AuthenticationPrincipal Tenant tenant) {
        return ResponseEntity.ok(ApiResponse.success(dashboardSettingsService.getSettings(tenant)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<DashboardSettingsDto>> saveSettings(
            @AuthenticationPrincipal Tenant tenant,
            @RequestBody DashboardSettingsDto request) {
        DashboardSettingsDto response = dashboardSettingsService.saveSettings(tenant, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Dashboard settings saved successfully"));
    }
}
