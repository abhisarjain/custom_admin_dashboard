package com.customadmindashboard.audit.controller;

import com.customadmindashboard.audit.dto.AuditLogResponse;
import com.customadmindashboard.audit.service.AuditService;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getProjectLogs(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Tenant tenant) {
        List<AuditLogResponse> logs = auditService.getProjectLogs(projectId, tenant);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/table/{tableName}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getTableLogs(
            @PathVariable Long projectId,
            @PathVariable String tableName,
            @AuthenticationPrincipal Tenant tenant) {
        List<AuditLogResponse> logs = auditService.getTableLogs(projectId, tableName, tenant);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}