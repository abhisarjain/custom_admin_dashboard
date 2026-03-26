package com.customadmindashboard.audit.service;

import com.customadmindashboard.audit.dto.AuditLogResponse;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.exception.BadRequestException;
import com.customadmindashboard.common.exception.ResourceNotFoundException;
import com.customadmindashboard.project.entity.Project;
import com.customadmindashboard.project.entity.ProjectMember;
import com.customadmindashboard.project.repository.ProjectMemberRepository;
import com.customadmindashboard.project.repository.ProjectRepository;
import com.customadmindashboard.rbac.entity.RolePermission;
import com.customadmindashboard.rbac.repository.RolePermissionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditLogWriter auditLogWriter;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Tenant tenant, Project project, String action,
                        String tableName, String recordId,
                        Object oldValue, Object newValue) {
        if (tenant == null || project == null || tenant.getId() == null || project.getId() == null) {
            return;
        }

        var event = new com.customadmindashboard.audit.event.AuditLogEvent(
                tenant.getId(),
                project.getId(),
                action,
                tableName,
                recordId,
                stringify(oldValue),
                stringify(newValue)
        );

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    writeSafely(event);
                }
            });
            return;
        }

        writeSafely(event);
    }

    public List<AuditLogResponse> getProjectLogs(Long projectId, Tenant tenant) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        validateAccess(project, tenant);

        return jdbcTemplate.query(
                """
                SELECT
                    al.id,
                    al.tenant_id,
                    t.name AS actor_name,
                    t.email AS actor_email,
                    al.project_id,
                    al.action,
                    al.table_name,
                    al.record_id,
                    al.old_value,
                    al.new_value,
                    al.created_at
                FROM audit_logs al
                JOIN tenants t ON t.id = al.tenant_id
                WHERE al.project_id = ?
                ORDER BY al.created_at DESC
                LIMIT 20
                """,
                this::mapRow,
                projectId
        );
    }

    public List<AuditLogResponse> getTableLogs(Long projectId, String tableName, Tenant tenant) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        validateAccess(project, tenant);

        return jdbcTemplate.query(
                """
                SELECT
                    al.id,
                    al.tenant_id,
                    t.name AS actor_name,
                    t.email AS actor_email,
                    al.project_id,
                    al.action,
                    al.table_name,
                    al.record_id,
                    al.old_value,
                    al.new_value,
                    al.created_at
                FROM audit_logs al
                JOIN tenants t ON t.id = al.tenant_id
                WHERE al.project_id = ? AND al.table_name = ?
                ORDER BY al.created_at DESC
                """,
                this::mapRow,
                projectId,
                tableName
        );
    }

    private AuditLogResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return AuditLogResponse.builder()
                .id(rs.getLong("id"))
                .tenantId(rs.getLong("tenant_id"))
                .actorName(rs.getString("actor_name"))
                .actorEmail(rs.getString("actor_email"))
                .projectId(rs.getLong("project_id"))
                .action(rs.getString("action"))
                .tableName(rs.getString("table_name"))
                .recordId(rs.getString("record_id"))
                .oldValue(stringValue(rs.getObject("old_value")))
                .newValue(stringValue(rs.getObject("new_value")))
                .createdAt(createdAt != null ? createdAt.toLocalDateTime() : null)
                .build();
    }

    private void validateAccess(Project project, Tenant tenant) {
        boolean isOwner = project.getTenant().getId().equals(tenant.getId());
        if (isOwner) {
            return;
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndTenantId(project.getId(), tenant.getId())
                .orElseThrow(() -> new BadRequestException("You do not have permission to view audit logs"));

        RolePermission rolePermission = rolePermissionRepository.findByRoleId(member.getRole().getId())
                .orElse(RolePermission.builder().build());

        if (!rolePermission.isCanViewAuditLogs()) {
            throw new BadRequestException("You do not have permission to view audit logs");
        }
    }

    private void writeSafely(com.customadmindashboard.audit.event.AuditLogEvent event) {
        try {
            auditLogWriter.write(event);
        } catch (Exception ex) {
            log.warn("Audit log skipped for action {}: {}", event.action(), ex.getMessage());
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            try {
                return objectMapper.writeValueAsString(stringValue);
            } catch (JsonProcessingException ex) {
                return "\"" + stringValue.replace("\"", "\\\"") + "\"";
            }
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
