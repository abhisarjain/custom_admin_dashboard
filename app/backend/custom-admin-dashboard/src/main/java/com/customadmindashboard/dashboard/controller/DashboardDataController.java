package com.customadmindashboard.dashboard.controller;

import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.response.ApiResponse;
import com.customadmindashboard.dashboard.dto.AddColumnRequest;
import com.customadmindashboard.dashboard.dto.CreateTableRequest;
import com.customadmindashboard.dashboard.dto.UpdateTableColumnRequest;
import com.customadmindashboard.database.service.DashboardDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/dashboard")
@RequiredArgsConstructor
public class DashboardDataController {

    private final DashboardDataService dashboardDataService;

    @GetMapping("/{viewId}/data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRows(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @AuthenticationPrincipal Tenant tenant) {
        return ResponseEntity.ok(ApiResponse.success(dashboardDataService.getRows(viewId, tenant)));
    }

    @PostMapping("/{viewId}/data")
    public ResponseEntity<ApiResponse<Void>> createRow(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal Tenant tenant) {
        dashboardDataService.createRow(viewId, data, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Row created successfully"));
    }

    @PutMapping("/{viewId}/data/{rowId}")
    public ResponseEntity<ApiResponse<Void>> updateRow(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @PathVariable Object rowId,
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal Tenant tenant) {
        dashboardDataService.updateRow(viewId, rowId, data, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Row updated successfully"));
    }

    @DeleteMapping("/{viewId}/data/{rowId}")
    public ResponseEntity<ApiResponse<Void>> deleteRow(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @PathVariable Object rowId,
            @AuthenticationPrincipal Tenant tenant) {
        dashboardDataService.deleteRow(viewId, rowId, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Row deleted successfully"));
    }

    @PostMapping("/tables")
    public ResponseEntity<ApiResponse<Void>> createTable(
            @PathVariable Long projectId,
            @RequestBody CreateTableRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        dashboardDataService.createTable(projectId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Table created successfully"));
    }

    @PostMapping("/{viewId}/columns")
    public ResponseEntity<ApiResponse<Void>> addColumn(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @RequestBody AddColumnRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        dashboardDataService.addColumn(viewId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Column added successfully"));
    }

    @DeleteMapping("/{viewId}/columns/{columnName}")
    public ResponseEntity<ApiResponse<Void>> deleteColumn(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @PathVariable String columnName,
            @AuthenticationPrincipal Tenant tenant) {
        dashboardDataService.deleteColumn(viewId, columnName, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Column deleted successfully"));
    }

    @PutMapping("/{viewId}/columns/{columnName}")
    public ResponseEntity<ApiResponse<Void>> updateColumn(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @PathVariable String columnName,
            @RequestBody UpdateTableColumnRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        request.setColumnName(columnName);
        dashboardDataService.updateColumn(viewId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Column updated successfully"));
    }

    @DeleteMapping("/{viewId}/table")
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @PathVariable Long projectId,
            @PathVariable Long viewId,
            @AuthenticationPrincipal Tenant tenant) {
        dashboardDataService.deleteTable(viewId, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Table deleted successfully"));
    }
}
