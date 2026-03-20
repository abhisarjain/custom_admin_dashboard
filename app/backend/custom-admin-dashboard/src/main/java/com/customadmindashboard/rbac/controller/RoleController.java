package com.customadmindashboard.rbac.controller;

import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.response.ApiResponse;
import com.customadmindashboard.rbac.dto.CreateRoleRequest;
import com.customadmindashboard.rbac.dto.RoleResponse;
import com.customadmindashboard.rbac.dto.SetPermissionsRequest;
import com.customadmindashboard.rbac.entity.RoleColumnPermission;
import com.customadmindashboard.rbac.entity.RoleMemberPermission;
import com.customadmindashboard.rbac.entity.RolePermission;
import com.customadmindashboard.rbac.entity.RoleTablePermission;
import com.customadmindashboard.rbac.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        RoleResponse response = roleService.createRole(projectId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(response, "Role created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Tenant tenant) {
        List<RoleResponse> roles = roleService.getRoles(projectId, tenant);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable Long projectId,
            @PathVariable Long roleId,
            @AuthenticationPrincipal Tenant tenant) {
        roleService.deleteRole(projectId, roleId, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
    }

    @PostMapping("/{roleId}/table-permissions")
    public ResponseEntity<ApiResponse<Void>> setTablePermission(
            @PathVariable Long projectId,
            @PathVariable Long roleId,
            @Valid @RequestBody SetPermissionsRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        roleService.setTablePermission(roleId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Table permission set successfully"));
    }

    @PostMapping("/{roleId}/column-permissions")
    public ResponseEntity<ApiResponse<Void>> setColumnPermission(
            @PathVariable Long projectId,
            @PathVariable Long roleId,
            @Valid @RequestBody SetPermissionsRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        roleService.setColumnPermission(roleId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Column permission set successfully"));
    }
    @GetMapping("/{roleId}/table-permissions")
public ResponseEntity<ApiResponse<List<RoleTablePermission>>> getTablePermissions(
        @PathVariable Long projectId,
        @PathVariable Long roleId,
        @AuthenticationPrincipal Tenant tenant) {
    return ResponseEntity.ok(ApiResponse.success(roleService.getTablePermissions(roleId)));
}

@GetMapping("/{roleId}/column-permissions")
public ResponseEntity<ApiResponse<List<RoleColumnPermission>>> getColumnPermissions(
        @PathVariable Long projectId,
        @PathVariable Long roleId,
        @AuthenticationPrincipal Tenant tenant) {
    return ResponseEntity.ok(ApiResponse.success(roleService.getColumnPermissions(roleId)));
}
// Role permissions
@GetMapping("/{roleId}/permissions")
public ResponseEntity<ApiResponse<RolePermission>> getRolePermissions(
        @PathVariable Long projectId,
        @PathVariable Long roleId,
        @AuthenticationPrincipal Tenant tenant) {
    return ResponseEntity.ok(ApiResponse.success(roleService.getRolePermissions(roleId)));
}

@PostMapping("/{roleId}/permissions")
public ResponseEntity<ApiResponse<Void>> setRolePermissions(
        @PathVariable Long projectId,
        @PathVariable Long roleId,
        @RequestBody RolePermission request,
        @AuthenticationPrincipal Tenant tenant) {
    roleService.setRolePermissions(roleId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "Role permissions saved!"));
}

// Member permissions
@GetMapping("/{roleId}/member-permissions")
public ResponseEntity<ApiResponse<RoleMemberPermission>> getMemberPermissions(
        @PathVariable Long projectId,
        @PathVariable Long roleId,
        @AuthenticationPrincipal Tenant tenant) {
    return ResponseEntity.ok(ApiResponse.success(roleService.getMemberPermissions(roleId)));
}

@PostMapping("/{roleId}/member-permissions")
public ResponseEntity<ApiResponse<Void>> setMemberPermissions(
        @PathVariable Long projectId,
        @PathVariable Long roleId,
        @RequestBody RoleMemberPermission request,
        @AuthenticationPrincipal Tenant tenant) {
    roleService.setMemberPermissions(roleId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "Member permissions saved!"));
}
@PutMapping("/{roleId}")
public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
        @PathVariable Long projectId,
        @PathVariable Long roleId,
        @RequestBody CreateRoleRequest request,
        @AuthenticationPrincipal Tenant tenant) {
    RoleResponse response = roleService.updateRole(roleId, request, tenant);
    return ResponseEntity.ok(ApiResponse.success(response, "Role updated successfully"));
}
}
