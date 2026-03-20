package com.customadmindashboard.rbac.service;

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
// Role permissions get karo
public RolePermission getRolePermissions(Long roleId) {
    return rolePermissionRepository.findByRoleId(roleId)
            .orElse(RolePermission.builder()
                    .canView(false).canCreate(false)
                    .canEdit(false).canDelete(false)
                    .build());
}

// Role permissions set karo
@Transactional
public void setRolePermissions(Long roleId, RolePermission request) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

    RolePermission permission = rolePermissionRepository.findByRoleId(roleId)
            .orElse(RolePermission.builder().role(role).build());

    permission.setCanView(request.isCanView());
    permission.setCanCreate(request.isCanCreate());
    permission.setCanEdit(request.isCanEdit());
    permission.setCanDelete(request.isCanDelete());

    rolePermissionRepository.save(permission);
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

    role.setName(nextName);
    role.setCanGrantView(request.isCanGrantView());
    role.setCanGrantCreate(request.isCanGrantCreate());
    role.setCanGrantEdit(request.isCanGrantEdit());
    role.setCanGrantDelete(request.isCanGrantDelete());
    role.setCanGrantDelegate(request.isCanGrantDelegate());

    role = roleRepository.save(role);
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
        roleRepository.delete(role);
    }

// Member permissions set karo
@Transactional
public void setMemberPermissions(Long roleId, RoleMemberPermission request) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

    RoleMemberPermission permission = roleMemberPermissionRepository.findByRoleId(roleId)
            .orElse(RoleMemberPermission.builder().role(role).build());

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
                .build();

        rolePermissionRepository.save(permission);

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
    }

    // Column level permissions set karo
    @Transactional
    public void setColumnPermission(Long roleId, SetPermissionsRequest request, Tenant tenant) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (request.getColumnName() == null || request.getColumnName().isBlank()) {
            throw new BadRequestException("Column name is required");
        }

        RoleColumnPermission permission = roleColumnPermissionRepository
                .findByRoleIdAndTableNameAndColumnName(roleId, request.getTableName(), request.getColumnName())
                .orElse(RoleColumnPermission.builder()
                        .role(role)
                        .tableName(request.getTableName())
                        .columnName(request.getColumnName())
                        .build());

        permission.setCanView(request.isCanView());
        permission.setCanEdit(request.isCanEdit());

        roleColumnPermissionRepository.save(permission);
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
