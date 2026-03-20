package com.customadmindashboard.project.controller;

import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.response.ApiResponse;
import com.customadmindashboard.project.dto.CreateProjectRequest;
import com.customadmindashboard.project.dto.ProjectResponse;
import com.customadmindashboard.project.dto.UpdateMemberRoleRequest;
import com.customadmindashboard.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        ProjectResponse response = projectService.createProject(request, tenant);
        return ResponseEntity.ok(ApiResponse.success(response, "Project created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects(
            @AuthenticationPrincipal Tenant tenant) {
        List<ProjectResponse> projects = projectService.getMyProjects(tenant);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Tenant tenant) {
        ProjectResponse response = projectService.getProject(projectId, tenant);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Tenant tenant) {
        projectService.deleteProject(projectId, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Project deleted successfully"));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMembers(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Tenant tenant) {
        List<Map<String, Object>> members = projectService.getMembers(projectId, tenant);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PutMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        projectService.updateMemberRole(projectId, memberId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Member role updated successfully"));
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal Tenant tenant) {
        projectService.removeMember(projectId, memberId, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully"));
    }

    @GetMapping("/{projectId}/my-permissions")
public ResponseEntity<ApiResponse<Map<String, Object>>> getMyPermissions(
        @PathVariable Long projectId,
        @AuthenticationPrincipal Tenant tenant) {
    return ResponseEntity.ok(ApiResponse.success(projectService.getMyPermissions(projectId, tenant)));
}
}
