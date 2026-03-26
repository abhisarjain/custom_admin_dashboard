package com.customadmindashboard.rbac.service;

import com.customadmindashboard.audit.service.AuditService;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.auth.repository.InvitationRepository;
import com.customadmindashboard.common.exception.BadRequestException;
import com.customadmindashboard.common.exception.ResourceNotFoundException;
import com.customadmindashboard.project.entity.Project;
import com.customadmindashboard.project.entity.ProjectMember;
import com.customadmindashboard.project.repository.ProjectMemberRepository;
import com.customadmindashboard.project.repository.ProjectRepository;
import com.customadmindashboard.rbac.dto.CreateRoleRequest;
import com.customadmindashboard.rbac.dto.RoleResponse;
import com.customadmindashboard.rbac.dto.SetPermissionsRequest;
import com.customadmindashboard.rbac.entity.*;
import com.customadmindashboard.rbac.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleTablePermissionRepository roleTablePermissionRepository;
    private final RoleColumnPermissionRepository roleColumnPermissionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RoleMemberPermissionRepository roleMemberPermissionRepository;
    private final InvitationRepository invitationRepository;
    private final AuditService auditService;
// Role permissions get karo
public RolePermission getRolePermissions(Long roleId) {
    return rolePermissionRepository.findByRoleId(roleId)
            .orElse(RolePermission.builder()
                    .canView(false).canCreate(false)
                    .canEdit(false).canDelete(false)
                    .canViewAuditLogs(false)
                    .build());
}

// Role permissions set karo
@Transactional
public void setRolePermissions(Long roleId, RolePermission request, Tenant tenant) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    boolean isOwner = role.getProject().getTenant().getId().equals(tenant.getId());

    RolePermission permission = rolePermissionRepository.findByRoleId(roleId)
            .orElse(RolePermission.builder().role(role).build());

    Map<String, Object> previousRolePermissionState = new LinkedHashMap<>();
    previousRolePermissionState.put("roleName", role.getName());
    previousRolePermissionState.put("canView", permission.isCanView());
    previousRolePermissionState.put("canCreate", permission.isCanCreate());
    previousRolePermissionState.put("canEdit", permission.isCanEdit());
    previousRolePermissionState.put("canDelete", permission.isCanDelete());
    previousRolePermissionState.put("canViewAuditLogs", permission.isCanViewAuditLogs());

    if (!isOwner && permission.isCanViewAuditLogs() != request.isCanViewAuditLogs()) {
        throw new BadRequestException("Only the project owner can manage audit log access");
    }

    permission.setCanView(request.isCanView());
    permission.setCanCreate(request.isCanCreate());
    permission.setCanEdit(request.isCanEdit());
    permission.setCanDelete(request.isCanDelete());
    permission.setCanViewAuditLogs(request.isCanViewAuditLogs());

    rolePermissionRepository.save(permission);

    Map<String, Object> rolePermissionState = new LinkedHashMap<>();
    rolePermissionState.put("roleName", role.getName());
    rolePermissionState.put("canView", permission.isCanView());
    rolePermissionState.put("canCreate", permission.isCanCreate());
    rolePermissionState.put("canEdit", permission.isCanEdit());
    rolePermissionState.put("canDelete", permission.isCanDelete());
    rolePermissionState.put("canViewAuditLogs", permission.isCanViewAuditLogs());
    auditService.publish(
            tenant,
            role.getProject(),
            "ROLE_PERMISSION_UPDATED",
            null,
            String.valueOf(roleId),
            previousRolePermissionState,
            rolePermissionState
    );
}

// Member permissions get karo
public RoleMemberPermission getMemberPermissions(Long roleId) {
    return roleMemberPermissionRepository.findByRoleId(roleId)
            .orElse(RoleMemberPermission.builder()
                    .canInvite(false).canView(false).canEdit(false).canRemove(false)
                    .grantInvite(false).grantView(false)
                    .grantEdit(false).grantRemove(false).grantDelegate(false)
                    .build());
}
@Transactional
public RoleResponse updateRole(Long roleId, CreateRoleRequest request, Tenant tenant) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

    Project project = role.getProject();
    boolean isOwner = project.getTenant().getId().equals(tenant.getId());
    boolean canEditRole = false;

    if (!isOwner) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndTenantId(project.getId(), tenant.getId())
                .orElseThrow(() -> new BadRequestException("Access denied"));

        canEditRole = rolePermissionRepository.findByRoleId(member.getRole().getId())
                .map(RolePermission::isCanEdit)
                .orElse(false);
    }

    if (!isOwner && !canEditRole) {
        throw new BadRequestException("Access denied");
    }

    String nextName = request.getName() == null ? role.getName() : request.getName().trim();
    if (nextName.isBlank()) {
        nextName = role.getName();
    }

    if (!role.getName().equals(nextName) && roleRepository.existsByNameAndProjectId(nextName, project.getId())) {
        throw new BadRequestException("Role with this name already exists");
    }

    Map<String, Object> previousRoleState = new LinkedHashMap<>();
    previousRoleState.put("roleName", role.getName());
    previousRoleState.put("canGrantView", role.isCanGrantView());
    previousRoleState.put("canGrantCreate", role.isCanGrantCreate());
    previousRoleState.put("canGrantEdit", role.isCanGrantEdit());
    previousRoleState.put("canGrantDelete", role.isCanGrantDelete());
    previousRoleState.put("canGrantDelegate", role.isCanGrantDelegate());

    role.setName(nextName);
    role.setCanGrantView(request.isCanGrantView());
    role.setCanGrantCreate(request.isCanGrantCreate());
    role.setCanGrantEdit(request.isCanGrantEdit());
    role.setCanGrantDelete(request.isCanGrantDelete());
    role.setCanGrantDelegate(request.isCanGrantDelegate());

    role = roleRepository.save(role);
    Map<String, Object> nextRoleState = new LinkedHashMap<>();
    nextRoleState.put("roleName", role.getName());
    nextRoleState.put("canGrantView", role.isCanGrantView());
    nextRoleState.put("canGrantCreate", role.isCanGrantCreate());
    nextRoleState.put("canGrantEdit", role.isCanGrantEdit());
    nextRoleState.put("canGrantDelete", role.isCanGrantDelete());
    nextRoleState.put("canGrantDelegate", role.isCanGrantDelegate());
    auditService.publish(
            tenant,
            project,
            "ROLE_UPDATED",
            null,
            String.valueOf(role.getId()),
            previousRoleState,
            nextRoleState
    );
    return mapToResponse(role);
}

    @Transactional
    public void deleteRole(Long projectId, Long roleId, Tenant tenant) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (!role.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Role not found");
        }

        Project project = role.getProject();
        boolean isOwner = project.getTenant().getId().equals(tenant.getId());
        boolean canDeleteRole = false;

        if (!isOwner) {
            ProjectMember member = projectMemberRepository.findByProjectIdAndTenantId(projectId, tenant.getId())
                    .orElseThrow(() -> new BadRequestException("Access denied"));

            canDeleteRole = rolePermissionRepository.findByRoleId(member.getRole().getId())
                    .map(RolePermission::isCanDelete)
                    .orElse(false);
        }

        if (!isOwner && !canDeleteRole) {
            throw new BadRequestException("Access denied");
        }

        if ("Super Admin".equalsIgnoreCase(role.getName())) {
            throw new BadRequestException("Super Admin role cannot be deleted");
        }

        if (projectMemberRepository.existsByRoleId(roleId)) {
            throw new BadRequestException("This role is assigned to members and cannot be deleted");
        }

        roleColumnPermissionRepository.deleteByRoleId(roleId);
        roleTablePermissionRepository.deleteByRoleId(roleId);
        roleMemberPermissionRepository.deleteByRoleId(roleId);
        rolePermissionRepository.deleteByRoleId(roleId);
        invitationRepository.deleteAllByRoleId(roleId);
        Map<String, Object> deletedRole = new LinkedHashMap<>();
        deletedRole.put("roleName", role.getName());
        auditService.publish(
                tenant,
                project,
                "ROLE_DELETED",
                null,
                String.valueOf(roleId),
                deletedRole,
                null
        );
        roleRepository.delete(role);
    }

// Member permissions set karo
@Transactional
public void setMemberPermissions(Long roleId, RoleMemberPermission request, Tenant tenant) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

    RoleMemberPermission permission = roleMemberPermissionRepository.findByRoleId(roleId)
            .orElse(RoleMemberPermission.builder().role(role).build());

    Map<String, Object> previousMemberPermissionState = new LinkedHashMap<>();
    previousMemberPermissionState.put("roleName", role.getName());
    previousMemberPermissionState.put("canInvite", permission.isCanInvite());
    previousMemberPermissionState.put("canView", permission.isCanView());
    previousMemberPermissionState.put("canEdit", permission.isCanEdit());
    previousMemberPermissionState.put("canRemove", permission.isCanRemove());
    previousMemberPermissionState.put("grantInvite", permission.isGrantInvite());
    previousMemberPermissionState.put("grantView", permission.isGrantView());
    previousMemberPermissionState.put("grantEdit", permission.isGrantEdit());
    previousMemberPermissionState.put("grantRemove", permission.isGrantRemove());
    previousMemberPermissionState.put("grantDelegate", permission.isGrantDelegate());

    permission.setCanInvite(request.isCanInvite());
    permission.setCanView(request.isCanView());
    permission.setCanEdit(request.isCanEdit());
    permission.setCanRemove(request.isCanRemove());
    permission.setGrantInvite(request.isGrantInvite());
    permission.setGrantView(request.isGrantView());
    permission.setGrantEdit(request.isGrantEdit());
    permission.setGrantRemove(request.isGrantRemove());
    permission.setGrantDelegate(request.isGrantDelegate());

    roleMemberPermissionRepository.save(permission);

    Map<String, Object> memberPermissionState = new LinkedHashMap<>();
    memberPermissionState.put("roleName", role.getName());
    memberPermissionState.put("canInvite", permission.isCanInvite());
    memberPermissionState.put("canView", permission.isCanView());
    memberPermissionState.put("canEdit", permission.isCanEdit());
    memberPermissionState.put("canRemove", permission.isCanRemove());
    memberPermissionState.put("grantInvite", permission.isGrantInvite());
    memberPermissionState.put("grantView", permission.isGrantView());
    memberPermissionState.put("grantEdit", permission.isGrantEdit());
    memberPermissionState.put("grantRemove", permission.isGrantRemove());
    memberPermissionState.put("grantDelegate", permission.isGrantDelegate());
    auditService.publish(
            tenant,
            role.getProject(),
            "MEMBER_PERMISSION_UPDATED",
            null,
            String.valueOf(roleId),
            previousMemberPermissionState,
            memberPermissionState
    );
}
    // Role banao
    @Transactional
    public RoleResponse createRole(Long projectId, CreateRoleRequest request, Tenant tenant) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isOwner = project.getTenant().getId().equals(tenant.getId());
        boolean canCreateRole = false;

        if (!isOwner) {
            ProjectMember member = projectMemberRepository.findByProjectIdAndTenantId(projectId, tenant.getId())
                    .orElseThrow(() -> new BadRequestException("Access denied"));

            canCreateRole = rolePermissionRepository.findByRoleId(member.getRole().getId())
                    .map(RolePermission::isCanCreate)
                    .orElse(false);
        }

        if (!isOwner && !canCreateRole) {
            throw new BadRequestException("Access denied");
        }

        // Same name already exists?
        if (roleRepository.existsByNameAndProjectId(request.getName(), projectId)) {
            throw new BadRequestException("Role with this name already exists");
        }

        Role role = Role.builder()
                .project(project)
                .name(request.getName())
                .canGrantView(request.isCanGrantView())
                .canGrantCreate(request.isCanGrantCreate())
                .canGrantEdit(request.isCanGrantEdit())
                .canGrantDelete(request.isCanGrantDelete())
                .canGrantDelegate(request.isCanGrantDelegate())
                .build();

        role = roleRepository.save(role);

        // Default permissions bhi banao
        RolePermission permission = RolePermission.builder()
                .role(role)
                .canView(false)
                .canCreate(false)
                .canEdit(false)
                .canDelete(false)
                .canViewAuditLogs(false)
                .build();

        rolePermissionRepository.save(permission);

        Map<String, Object> createdRole = new LinkedHashMap<>();
        createdRole.put("roleName", role.getName());
        createdRole.put("canGrantView", role.isCanGrantView());
        createdRole.put("canGrantCreate", role.isCanGrantCreate());
        createdRole.put("canGrantEdit", role.isCanGrantEdit());
        createdRole.put("canGrantDelete", role.isCanGrantDelete());
        createdRole.put("canGrantDelegate", role.isCanGrantDelegate());
        auditService.publish(
                tenant,
                project,
                "ROLE_CREATED",
                null,
                String.valueOf(role.getId()),
                null,
                createdRole
        );

        return mapToResponse(role);
    }

    // Saare roles dekho
  public List<RoleResponse> getRoles(Long projectId, Tenant tenant) {
    projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

    // Owner check hatao — sirf project exist check karo
    return roleRepository.findAllByProjectId(projectId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

    // Table level permissions set karo
    @Transactional
    public void setTablePermission(Long roleId, SetPermissionsRequest request, Tenant tenant) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Already exists? Update karo
        RoleTablePermission permission = roleTablePermissionRepository
                .findByRoleIdAndTableName(roleId, request.getTableName())
                .orElse(RoleTablePermission.builder()
                        .role(role)
                        .tableName(request.getTableName())
                        .build());

        Map<String, Object> previousTablePermissionState = new LinkedHashMap<>();
        previousTablePermissionState.put("roleName", role.getName());
        previousTablePermissionState.put("canViewData", permission.isCanViewData());
        previousTablePermissionState.put("canViewStructure", permission.isCanViewStructure());
        previousTablePermissionState.put("canCreateData", permission.isCanCreateData());
        previousTablePermissionState.put("canCreateStructure", permission.isCanCreateStructure());
        previousTablePermissionState.put("canEditData", permission.isCanEditData());
        previousTablePermissionState.put("canEditStructure", permission.isCanEditStructure());
        previousTablePermissionState.put("canDeleteData", permission.isCanDeleteData());
        previousTablePermissionState.put("canDeleteStructure", permission.isCanDeleteStructure());
        previousTablePermissionState.put("canDeleteTable", permission.isCanDeleteTable());
        previousTablePermissionState.put("canGrantDelegate", permission.isCanGrantDelegate());

        boolean canCreateData = request.isCanCreateData() || request.isCanCreate();
        boolean canCreateStructure = request.isCanCreateStructure();
        boolean canEditData = request.isCanEditData() || request.isCanEdit();
        boolean canEditStructure = request.isCanEditStructure() || request.isCanEdit();
        boolean canDeleteData = request.isCanDeleteData() || request.isCanDelete();
        boolean canDeleteStructure = request.isCanDeleteStructure() || request.isCanDelete();
        boolean canDeleteTable = request.isCanDeleteTable();
        boolean canCreate = canCreateData || canCreateStructure || request.isCanCreate();
        boolean canViewData = request.isCanViewData() || request.isCanView() || canCreateData || canEditData || canDeleteData;
        boolean canViewStructure = request.isCanViewStructure() || request.isCanView() || canCreateStructure || canEditStructure || canDeleteStructure || canDeleteTable;

        permission.setCanViewData(canViewData);
        permission.setCanViewStructure(canViewStructure);
        permission.setCanCreate(canCreate);
        permission.setCanCreateData(canCreateData);
        permission.setCanCreateStructure(canCreateStructure);
        permission.setCanEditData(canEditData);
        permission.setCanEditStructure(canEditStructure);
        permission.setCanDeleteData(canDeleteData);
        permission.setCanDeleteStructure(canDeleteStructure);
        permission.setCanDeleteTable(canDeleteTable);
        permission.setCanView(canViewData || canViewStructure);
        permission.setCanEdit(canEditData || canEditStructure);
        permission.setCanDelete(canDeleteData || canDeleteStructure || canDeleteTable);
        permission.setCanGrantView(request.isCanGrantView());
        permission.setCanGrantCreate(request.isCanGrantCreate());
        permission.setCanGrantEdit(request.isCanGrantEdit());
        permission.setCanGrantDelete(request.isCanGrantDelete());
        permission.setCanGrantDelegate(request.isCanGrantDelegate());

        roleTablePermissionRepository.save(permission);

        Map<String, Object> tablePermissionState = new LinkedHashMap<>();
        tablePermissionState.put("roleName", role.getName());
        tablePermissionState.put("canViewData", permission.isCanViewData());
        tablePermissionState.put("canViewStructure", permission.isCanViewStructure());
        tablePermissionState.put("canCreateData", permission.isCanCreateData());
        tablePermissionState.put("canCreateStructure", permission.isCanCreateStructure());
        tablePermissionState.put("canEditData", permission.isCanEditData());
        tablePermissionState.put("canEditStructure", permission.isCanEditStructure());
        tablePermissionState.put("canDeleteData", permission.isCanDeleteData());
        tablePermissionState.put("canDeleteStructure", permission.isCanDeleteStructure());
        tablePermissionState.put("canDeleteTable", permission.isCanDeleteTable());
        tablePermissionState.put("canGrantDelegate", permission.isCanGrantDelegate());
        auditService.publish(
                tenant,
                role.getProject(),
                "TABLE_PERMISSION_UPDATED",
                request.getTableName(),
                String.valueOf(roleId),
                previousTablePermissionState,
                tablePermissionState
        );
    }

    // Column level permissions set karo
    @Transactional
    public void setColumnPermission(Long roleId, SetPermissionsRequest request, Tenant tenant) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (request.getColumnName() == null || request.getColumnName().isBlank()) {
            throw new BadRequestException("Column name is required");
        }

        List<RoleColumnPermission> existingPermissions = roleColumnPermissionRepository
                .findAllByRoleIdAndTableNameAndColumnName(roleId, request.getTableName(), request.getColumnName());

        RoleColumnPermission permission = existingPermissions.stream().findFirst()
                .orElse(RoleColumnPermission.builder()
                        .role(role)
                        .tableName(request.getTableName())
                        .columnName(request.getColumnName())
                        .build());

        Map<String, Object> previousColumnPermissionState = new LinkedHashMap<>();
        previousColumnPermissionState.put("roleName", role.getName());
        previousColumnPermissionState.put("columnName", request.getColumnName());
        previousColumnPermissionState.put("canView", permission.isCanView());
        previousColumnPermissionState.put("canCreate", permission.isCanCreate());
        previousColumnPermissionState.put("canEdit", permission.isCanEdit());
        previousColumnPermissionState.put("canDelete", permission.isCanDelete());

        if (existingPermissions.size() > 1) {
            roleColumnPermissionRepository.deleteAll(
                    existingPermissions.subList(1, existingPermissions.size())
            );
        }

        permission.setCanView(request.isCanView());
        permission.setCanCreate(request.isCanCreate());
        permission.setCanEdit(request.isCanEdit());
        permission.setCanDelete(request.isCanDelete());
        permission.setCanGrantView(false);
        permission.setCanGrantEdit(false);

        roleColumnPermissionRepository.saveAndFlush(permission);

        Map<String, Object> columnPermissionState = new LinkedHashMap<>();
        columnPermissionState.put("roleName", role.getName());
        columnPermissionState.put("columnName", request.getColumnName());
        columnPermissionState.put("canView", permission.isCanView());
        columnPermissionState.put("canCreate", permission.isCanCreate());
        columnPermissionState.put("canEdit", permission.isCanEdit());
        columnPermissionState.put("canDelete", permission.isCanDelete());
        auditService.publish(
                tenant,
                role.getProject(),
                "COLUMN_PERMISSION_UPDATED",
                request.getTableName(),
                String.valueOf(roleId),
                previousColumnPermissionState,
                columnPermissionState
        );
    }

    private RoleResponse mapToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .projectId(role.getProject().getId())
                .canGrantView(role.isCanGrantView())
                .canGrantCreate(role.isCanGrantCreate())
                .canGrantEdit(role.isCanGrantEdit())
                .canGrantDelete(role.isCanGrantDelete())
                .canGrantDelegate(role.isCanGrantDelegate())
                .createdAt(role.getCreatedAt())
                .build();
    }
    // Table permissions get karo
public List<RoleTablePermission> getTablePermissions(Long roleId) {
    return roleTablePermissionRepository.findAllByRoleId(roleId);
}

// Column permissions get karo
public List<RoleColumnPermission> getColumnPermissions(Long roleId) {
    return roleColumnPermissionRepository.findAllByRoleId(roleId);
}
}
