package com.customadmindashboard.auth.controller;

import com.customadmindashboard.auth.dto.AcceptInviteRequest;
import com.customadmindashboard.auth.dto.InvitationResponse;
import com.customadmindashboard.auth.dto.InviteRequest;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.auth.service.InvitationService;
import com.customadmindashboard.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    // Invite bhejo
    @PostMapping("/api/projects/{projectId}/invite")
    public ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(
            @PathVariable Long projectId,
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        InvitationResponse response = invitationService.createInvitation(projectId, request, tenant);
        return ResponseEntity.ok(ApiResponse.success(response, "Invitation sent successfully"));
    }

    // Meri invitations dekho
    @GetMapping("/api/invitations/my")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> getMyInvitations(
            @AuthenticationPrincipal Tenant tenant) {
        List<InvitationResponse> invitations = invitationService.getMyInvitations(tenant);
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    // Accept karo
    @PostMapping("/api/invitations/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @Valid @RequestBody AcceptInviteRequest request,
            @AuthenticationPrincipal Tenant tenant) {
        invitationService.acceptInvitation(request, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation accepted successfully"));
    }

    @DeleteMapping("/api/projects/{projectId}/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(
            @PathVariable Long projectId,
            @PathVariable Long invitationId,
            @AuthenticationPrincipal Tenant tenant) {
        invitationService.cancelInvitation(projectId, invitationId, tenant);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation cancelled successfully"));
    }
}
