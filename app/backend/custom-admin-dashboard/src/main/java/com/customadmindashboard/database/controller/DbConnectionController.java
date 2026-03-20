package com.customadmindashboard.database.controller;

import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.response.ApiResponse;
import com.customadmindashboard.database.dto.CreateDbConnectionRequest;
import com.customadmindashboard.database.dto.DbConnectionResponse;
import com.customadmindashboard.database.dto.DbSchemaResponse;
import com.customadmindashboard.database.service.DbConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/connections")
@RequiredArgsConstructor
public class DbConnectionController {

    private final DbConnectionService dbConnectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<DbConnectionResponse>> createConnection(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateDbConnectionRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        DbConnectionResponse response = dbConnectionService.createConnection(projectId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(response, "Database connected successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DbConnectionResponse>>> getConnections(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Tenant tenant) {
        List<DbConnectionResponse> connections = dbConnectionService.getConnections(projectId, tenant);
        return ResponseEntity.ok(ApiResponse.success(connections));
    }

    @PostMapping("/{connectionId}/sync")
    public ResponseEntity<ApiResponse<List<DbSchemaResponse>>> syncSchema(
            @PathVariable Long projectId,
            @PathVariable Long connectionId,
            @AuthenticationPrincipal Tenant tenant) {
        List<DbSchemaResponse> schema = dbConnectionService.syncSchema(connectionId, tenant);
        return ResponseEntity.ok(ApiResponse.success(schema, "Schema synced successfully"));
    }

    @GetMapping("/{connectionId}/schema")
    public ResponseEntity<ApiResponse<List<DbSchemaResponse>>> getSchema(
            @PathVariable Long projectId,
            @PathVariable Long connectionId,
            @AuthenticationPrincipal Tenant tenant) {
        List<DbSchemaResponse> schema = dbConnectionService.getSchema(connectionId, tenant);
        return ResponseEntity.ok(ApiResponse.success(schema));
    }
}