package com.customadmindashboard.project.service;

import com.customadmindashboard.audit.repository.AuditLogRepository;
import com.customadmindashboard.auth.repository.ApiTokenRepository;
import com.customadmindashboard.auth.repository.InvitationRepository;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.exception.BadRequestException;
import com.customadmindashboard.common.exception.ResourceNotFoundException;
import com.customadmindashboard.dashboard.entity.DashboardView;
import com.customadmindashboard.dashboard.repository.DashboardColumnRepository;
import com.customadmindashboard.dashboard.repository.DashboardViewRepository;
import com.customadmindashboard.database.entity.DbConnection;
import com.customadmindashboard.database.repository.DbConnectionRepository;
import com.customadmindashboard.database.repository.DbSchemaRepository;
import com.customadmindashboard.database.repository.DbSyncLogRepository;
import com.customadmindashboard.project.dto.CreateProjectRequest;
import com.customadmindashboard.project.dto.ProjectResponse;
import com.customadmindashboard.project.dto.UpdateMemberRoleRequest;
import com.customadmindashboard.project.entity.Project;
import com.customadmindashboard.project.entity.ProjectMember;
import com.customadmindashboard.project.repository.ProjectMemberRepository;
import com.customadmindashboard.project.repository.ProjectRepository;
import com.customadmindashboard.rbac.entity.Role;
import com.customadmindashboard.rbac.entity.RolePermission;
import com.customadmindashboard.rbac.entity.RoleMemberPermission;
import com.customadmindashboard.rbac.repository.MemberPermissionGrantRepository;
import com.customadmindashboard.rbac.repository.RoleMemberPermissionRepository;
import com.customadmindashboard.rbac.repository.RolePermissionRepository;
import com.customadmindashboard.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


import com.customadmindashboard.rbac.entity.RoleTablePermission;
import com.customadmindashboard.rbac.entity.RoleColumnPermission;
import com.customadmindashboard.rbac.repository.RoleTablePermissionRepository;
import com.customadmindashboard.rbac.repository.RoleColumnPermissionRepository;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleMemberPermissionRepository roleMemberPermissionRepository;
    private final MemberPermissionGrantRepository memberPermissionGrantRepository;
    private final InvitationRepository invitationRepository;
    private final ApiTokenRepository apiTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final DashboardViewRepository dashboardViewRepository;
    private final DashboardColumnRepository dashboardColumnRepository;
    private final DbConnectionRepository dbConnectionRepository;
    private final DbSchemaRepository dbSchemaRepository;
    private final DbSyncLogRepository dbSyncLogRepository;

    private final RoleTablePermissionRepository roleTablePermissionRepository;
private final RoleColumnPermissionRepository roleColumnPermissionRepository;

    // Project banao
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, Tenant tenant) {

        // Same name ka project already exists?
        if (projectRepository.existsByNameAndTenantId(request.getName(), tenant.getId())) {
            throw new BadRequestException("Project with this name already exists");
        }

        // Project save karo
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tenant(tenant)
                .build();

        project = projectRepository.save(project);

        // Super Admin role banao automatically
        Role superAdmin = Role.builder()
                .project(project)
                .name("Super Admin")
                .canGrantView(true)
                .canGrantCreate(true)
                .canGrantEdit(true)
                .canGrantDelete(true)
                .canGrantDelegate(true)
                .build();

        superAdmin = roleRepository.save(superAdmin);

        rolePermissionRepository.save(RolePermission.builder()
                .role(superAdmin)
                .canView(true)
                .canCreate(true)
                .canEdit(true)
                .canDelete(true)
                .build());

        roleMemberPermissionRepository.save(RoleMemberPermission.builder()
                .role(superAdmin)
                .canInvite(true)
                .canView(true)
                .canEdit(true)
                .canRemove(true)
                .grantInvite(true)
                .grantView(true)
                .grantEdit(true)
                .grantRemove(true)
                .grantDelegate(true)
                .build());

        roleTablePermissionRepository.save(RoleTablePermission.builder()
                .role(superAdmin)
                .tableName("__new_table__")
                .canView(true)
                .canViewData(true)
                .canViewStructure(true)
                .canCreate(true)
                .canCreateData(true)
                .canCreateStructure(true)
                .canEdit(true)
                .canEditData(true)
                .canEditStructure(true)
                .canDelete(true)
                .canDeleteData(true)
                .canDeleteStructure(true)
                .canDeleteTable(true)
                .canGrantView(true)
                .canGrantCreate(true)
                .canGrantEdit(true)
                .canGrantDelete(true)
                .canGrantDelegate(true)
                .build());

        // Creator ko Super Admin banao
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .tenant(tenant)
                .role(superAdmin)
                .build();

        projectMemberRepository.save(member);

        return mapToResponse(project);
    }

    // Saare projects dekho
   public List<ProjectResponse> getMyProjects(Tenant tenant) {
    // Apne banaye projects
    List<Project> ownProjects = projectRepository.findAllByTenantId(tenant.getId());
    
    // Jisme member hai woh projects
    List<Project> memberProjects = projectMemberRepository
            .findAllByTenantId(tenant.getId())
            .stream()
            .map(ProjectMember::getProject)
            .toList();

    // Dono merge karo — duplicates remove karo
    Set<Long> seen = new HashSet<>();
    List<Project> allProjects = new ArrayList<>();
    
    for (Project p : ownProjects) {
        if (seen.add(p.getId())) allProjects.add(p);
    }
    for (Project p : memberProjects) {
        if (seen.add(p.getId())) allProjects.add(p);
    }

    return allProjects.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

    // Single project dekho
    public ProjectResponse getProject(Long projectId, Tenant tenant) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

    // Owner hai ya member hai — dono allow karo
    boolean isOwner = project.getTenant().getId().equals(tenant.getId());
    boolean isMember = projectMemberRepository
            .existsByProjectIdAndTenantId(projectId, tenant.getId());

    if (!isOwner && !isMember) {
        throw new ResourceNotFoundException("Project not found");
    }

    return mapToResponse(project);
}

    // Members dekho
public List<Map<String, Object>> getMembers(Long projectId, Tenant tenant) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

    List<Map<String, Object>> memberRows = projectMemberRepository.findAllByProjectId(projectId)
            .stream()
            .map(member -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", member.getId());
                map.put("entryType", "member");
                map.put("tenantId", member.getTenant().getId());
                map.put("name", member.getTenant().getName());
                map.put("email", member.getTenant().getEmail());
                map.put("role", member.getRole().getName());
                map.put("roleId", member.getRole().getId());
                map.put("isOwner", member.getTenant().getId().equals(project.getTenant().getId()));
                map.put("status", "active");
                map.put("isPending", false);
                return map;
            })
            .collect(Collectors.toList());

    List<Map<String, Object>> pendingRows = invitationRepository.findAllByProjectId(projectId)
            .stream()
            .filter(inv -> "pending".equalsIgnoreCase(inv.getStatus()))
            .map(inv -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", "pending-" + inv.getId());
                map.put("invitationId", inv.getId());
                map.put("entryType", "invitation");
                map.put("tenantId", null);
                map.put("name", inv.getEmail());
                map.put("email", inv.getEmail());
                map.put("role", inv.getRole().getName());
                map.put("roleId", inv.getRole().getId());
                map.put("isOwner", false);
                map.put("status", "pending");
                map.put("isPending", true);
                return map;
            })
            .collect(Collectors.toList());

    memberRows.addAll(pendingRows);
    return memberRows;
}

    @Transactional
    public void updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, Tenant tenant) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isOwner = project.getTenant().getId().equals(tenant.getId());
        boolean canEditMembers = false;

        if (!isOwner) {
            ProjectMember currentMember = projectMemberRepository.findByProjectIdAndTenantId(projectId, tenant.getId())
                    .orElseThrow(() -> new BadRequestException("Access denied"));

            RoleMemberPermission memberPermission = roleMemberPermissionRepository
                    .findByRoleId(currentMember.getRole().getId())
                    .orElse(RoleMemberPermission.builder().build());
            canEditMembers = memberPermission.isCanEdit();
        }

        if (!isOwner && !canEditMembers) {
            throw new BadRequestException("Access denied");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!member.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Member not found");
        }

        Role nextRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (!nextRole.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Role does not belong to this project");
        }

        member.setRole(nextRole);
        projectMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long projectId, Long memberId, Tenant tenant) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isOwner = project.getTenant().getId().equals(tenant.getId());
        boolean canRemoveMembers = false;

        if (!isOwner) {
            ProjectMember currentMember = projectMemberRepository.findByProjectIdAndTenantId(projectId, tenant.getId())
                    .orElseThrow(() -> new BadRequestException("Access denied"));

            RoleMemberPermission memberPermission = roleMemberPermissionRepository
                    .findByRoleId(currentMember.getRole().getId())
                    .orElse(RoleMemberPermission.builder().build());
            canRemoveMembers = memberPermission.isCanRemove();
        }

        if (!isOwner && !canRemoveMembers) {
            throw new BadRequestException("Access denied");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!member.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Member not found");
        }

        if (member.getTenant().getId().equals(project.getTenant().getId())) {
            throw new BadRequestException("Project owner cannot be removed");
        }

        projectMemberRepository.delete(member);
    }
    

    // Delete project
    @Transactional
    public void deleteProject(Long projectId, Tenant tenant) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getTenant().getId().equals(tenant.getId())) {
            throw new ResourceNotFoundException("Project not found");
        }

        invitationRepository.deleteAllByProjectId(projectId);
        apiTokenRepository.deleteAllByProjectId(projectId);
        auditLogRepository.deleteAllByProjectId(projectId);
        memberPermissionGrantRepository.deleteAllByProjectId(projectId);

        List<DashboardView> dashboardViews = dashboardViewRepository.findAllByProjectId(projectId);
        for (DashboardView view : dashboardViews) {
            dashboardColumnRepository.deleteAllByDashboardViewId(view.getId());
        }
        dashboardViewRepository.deleteAllByProjectId(projectId);

        List<DbConnection> connections = dbConnectionRepository.findAllByProjectId(projectId);
        for (DbConnection connection : connections) {
            dbSyncLogRepository.deleteAllByDbConnectionId(connection.getId());
            dbSchemaRepository.deleteAllByDbConnectionId(connection.getId());
        }
        dbConnectionRepository.deleteAllByProjectId(projectId);

        List<Role> roles = roleRepository.findAllByProjectId(projectId);
        projectMemberRepository.deleteAllByProjectId(projectId);
        for (Role role : roles) {
            roleColumnPermissionRepository.deleteByRoleId(role.getId());
            roleTablePermissionRepository.deleteByRoleId(role.getId());
            roleMemberPermissionRepository.deleteByRoleId(role.getId());
            rolePermissionRepository.deleteByRoleId(role.getId());
        }
        roleRepository.deleteAllByProjectId(projectId);

        projectRepository.delete(project);
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .tenantId(project.getTenant().getId())
                .createdAt(project.getCreatedAt())
                .build();
    }


    public Map<String, Object> getMyPermissions(Long projectId, Tenant tenant) {
        
    // Member dhundo
    ProjectMember member = projectMemberRepository
            .findByProjectIdAndTenantId(projectId, tenant.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Not a member of this project"));

    Role role = member.getRole();

    // Table permissions
    List<RoleTablePermission> tablePerms = roleTablePermissionRepository
            .findAllByRoleId(role.getId());

    // Column permissions
    List<RoleColumnPermission> columnPerms = roleColumnPermissionRepository
            .findAllByRoleId(role.getId());

    // Default permissions
    Map<String, Object> defaultPerms = new HashMap<>();
    tablePerms.stream()
            .filter(p -> p.getTableName().equals("__default__"))
            .findFirst()
            .ifPresent(p -> {
                defaultPerms.put("canView", p.isCanView());
                defaultPerms.put("canCreate", p.isCanCreate());
                defaultPerms.put("canEdit", p.isCanEdit());
                defaultPerms.put("canDelete", p.isCanDelete());
            });

    // Table level permissions
    Map<String, Object> tablePermMap = new HashMap<>();
    tablePerms.stream()
            .filter(p -> !p.getTableName().equals("__default__") && !p.getTableName().equals("*"))
            .forEach(p -> {
                Map<String, Boolean> perms = new HashMap<>();
                perms.put("canView", p.isCanView());
                perms.put("canViewData", p.isCanViewData());
                perms.put("canViewStructure", p.isCanViewStructure());
                perms.put("canCreate", p.isCanCreate());
                perms.put("canCreateData", p.isCanCreateData());
                perms.put("canCreateStructure", p.isCanCreateStructure());
                perms.put("canEdit", p.isCanEdit());
                perms.put("canEditData", p.isCanEditData());
                perms.put("canEditStructure", p.isCanEditStructure());
                perms.put("canDelete", p.isCanDelete());
                perms.put("canDeleteData", p.isCanDeleteData());
                perms.put("canDeleteStructure", p.isCanDeleteStructure());
                perms.put("canDeleteTable", p.isCanDeleteTable());
                perms.put("canGrantView", p.isCanGrantView());
                perms.put("canGrantCreate", p.isCanGrantCreate());
                perms.put("canGrantEdit", p.isCanGrantEdit());
                perms.put("canGrantDelete", p.isCanGrantDelete());
                perms.put("canGrantDelegate", p.isCanGrantDelegate());
                tablePermMap.put(p.getTableName(), perms);
            });

    // Column level permissions
    Map<String, Object> columnPermMap = new HashMap<>();
    columnPerms.forEach(p -> {
        String key = p.getTableName() + "." + p.getColumnName();
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("canView", p.isCanView());
        perms.put("canEdit", p.isCanEdit());
        columnPermMap.put(key, perms);
    });
    // Grant permissions bhi add karo
Map<String, Object> grantPermissions = new HashMap<>();
grantPermissions.put("canGrantView", role.isCanGrantView());
grantPermissions.put("canGrantCreate", role.isCanGrantCreate());
grantPermissions.put("canGrantEdit", role.isCanGrantEdit());
grantPermissions.put("canGrantDelete", role.isCanGrantDelete());
grantPermissions.put("canGrantDelegate", role.isCanGrantDelegate());



    Map<String, Object> result = new HashMap<>();
    result.put("role", role.getName());
    result.put("roleId", role.getId());
    result.put("defaultPermissions", defaultPerms);
    result.put("tablePermissions", tablePermMap);
    result.put("columnPermissions", columnPermMap);
    result.put("grantPermissions", grantPermissions);

    RoleMemberPermission memberPermission = roleMemberPermissionRepository.findByRoleId(role.getId())
            .orElse(RoleMemberPermission.builder().build());
    Map<String, Object> memberPermissions = new HashMap<>();
    memberPermissions.put("canInvite", memberPermission.isCanInvite());
    memberPermissions.put("canView", memberPermission.isCanView());
    memberPermissions.put("canEdit", memberPermission.isCanEdit());
    memberPermissions.put("canRemove", memberPermission.isCanRemove());
    memberPermissions.put("grantInvite", memberPermission.isGrantInvite());
    memberPermissions.put("grantView", memberPermission.isGrantView());
    memberPermissions.put("grantEdit", memberPermission.isGrantEdit());
    memberPermissions.put("grantRemove", memberPermission.isGrantRemove());
    memberPermissions.put("grantDelegate", memberPermission.isGrantDelegate());
    result.put("memberPermissions", memberPermissions);

    return result;
}
}
