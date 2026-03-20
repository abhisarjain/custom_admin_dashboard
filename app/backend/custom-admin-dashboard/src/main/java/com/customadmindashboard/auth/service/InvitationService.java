package com.customadmindashboard.auth.service;

import com.customadmindashboard.auth.dto.AcceptInviteRequest;
import com.customadmindashboard.auth.dto.InvitationResponse;
import com.customadmindashboard.auth.dto.InviteRequest;
import com.customadmindashboard.auth.entity.Invitation;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.auth.repository.InvitationRepository;
import com.customadmindashboard.common.exception.BadRequestException;
import com.customadmindashboard.common.exception.ResourceNotFoundException;
import com.customadmindashboard.project.entity.Project;
import com.customadmindashboard.project.entity.ProjectMember;
import com.customadmindashboard.project.repository.ProjectMemberRepository;
import com.customadmindashboard.project.repository.ProjectRepository;
import com.customadmindashboard.rbac.entity.Role;
import com.customadmindashboard.rbac.entity.RoleMemberPermission;
import com.customadmindashboard.rbac.repository.RoleMemberPermissionRepository;
import com.customadmindashboard.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RoleRepository roleRepository;
    private final RoleMemberPermissionRepository roleMemberPermissionRepository;

    // Invite bhejo
    @Transactional
    public InvitationResponse createInvitation(Long projectId,
            InviteRequest request, Tenant inviter) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isOwner = project.getTenant().getId().equals(inviter.getId());
        boolean canInvite = false;

        if (!isOwner) {
            ProjectMember inviterMember = projectMemberRepository.findByProjectIdAndTenantId(projectId, inviter.getId())
                    .orElseThrow(() -> new BadRequestException("Access denied"));

            RoleMemberPermission memberPermission = roleMemberPermissionRepository
                    .findByRoleId(inviterMember.getRole().getId())
                    .orElse(RoleMemberPermission.builder().build());
            canInvite = memberPermission.isCanInvite();
        }

        if (!isOwner && !canInvite) {
            throw new BadRequestException("Access denied");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (!role.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Role does not belong to this project");
        }

        boolean alreadyMember = projectMemberRepository.findAllByProjectId(projectId)
                .stream()
                .anyMatch(member -> member.getTenant().getEmail().equalsIgnoreCase(request.getEmail()));
        if (alreadyMember) {
            throw new BadRequestException("User is already a member of this project");
        }

        List<Invitation> existingInvitations = invitationRepository.findAllByEmailAndProjectId(request.getEmail(), projectId);
        boolean hasActiveInvitation = existingInvitations.stream()
                .anyMatch(inv -> "pending".equalsIgnoreCase(inv.getStatus())
                        && inv.getExpiresAt() != null
                        && inv.getExpiresAt().isAfter(LocalDateTime.now()));

        if (hasActiveInvitation) {
            throw new BadRequestException("User already invited to this project");
        }

        if (!existingInvitations.isEmpty()) {
            invitationRepository.deleteAll(existingInvitations);
        }

        // Token generate karo
        String token = UUID.randomUUID().toString();

        Invitation invitation = Invitation.builder()
                .project(project)
                .invitedBy(inviter)
                .role(role)
                .email(request.getEmail())
                .token(token)
                .status("pending")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        invitation = invitationRepository.save(invitation);

        return mapToResponse(invitation);
    }

    // Meri pending invitations dekho
    public List<InvitationResponse> getMyInvitations(Tenant tenant) {
        return invitationRepository.findAllByEmail(tenant.getEmail())
                .stream()
                .filter(inv -> inv.getStatus().equals("pending"))
                .filter(inv -> inv.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Accept karo
    @Transactional
    public void acceptInvitation(AcceptInviteRequest request, Tenant tenant) {

        Invitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        // Expire check karo
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invitation has expired");
        }

        // Already accepted?
        if (!invitation.getStatus().equals("pending")) {
            throw new BadRequestException("Invitation already " + invitation.getStatus());
        }

        // Email match karo
        if (!invitation.getEmail().equals(tenant.getEmail())) {
            throw new BadRequestException("This invitation is for a different email");
        }

        // Already member?
        if (projectMemberRepository.existsByProjectIdAndTenantId(
                invitation.getProject().getId(), tenant.getId())) {
            throw new BadRequestException("Already a member of this project");
        }

        // Project member banao
        ProjectMember member = ProjectMember.builder()
                .project(invitation.getProject())
                .tenant(tenant)
                .role(invitation.getRole())
                .build();

        projectMemberRepository.save(member);

        // Status update karo
        invitation.setStatus("accepted");
        invitationRepository.save(invitation);
    }

    @Transactional
    public void cancelInvitation(Long projectId, Long invitationId, Tenant tenant) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Invitation not found");
        }

        Project project = invitation.getProject();
        boolean isOwner = project.getTenant().getId().equals(tenant.getId());
        boolean canInvite = false;

        if (!isOwner) {
            ProjectMember inviterMember = projectMemberRepository.findByProjectIdAndTenantId(projectId, tenant.getId())
                    .orElseThrow(() -> new BadRequestException("Access denied"));

            RoleMemberPermission memberPermission = roleMemberPermissionRepository
                    .findByRoleId(inviterMember.getRole().getId())
                    .orElse(RoleMemberPermission.builder().build());
            canInvite = memberPermission.isCanInvite();
        }

        if (!isOwner && !canInvite) {
            throw new BadRequestException("Access denied");
        }

        invitationRepository.delete(invitation);
    }

    private InvitationResponse mapToResponse(Invitation inv) {
        return InvitationResponse.builder()
                .id(inv.getId())
                .projectId(inv.getProject().getId())
                .projectName(inv.getProject().getName())
                .invitedByName(inv.getInvitedBy().getName())
                .roleName(inv.getRole().getName())
                .email(inv.getEmail())
                .token(inv.getToken())
                .status(inv.getStatus())
                .expiresAt(inv.getExpiresAt())
                .createdAt(inv.getCreatedAt())
                .build();
    }
}
