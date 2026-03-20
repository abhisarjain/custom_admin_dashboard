package com.customadmindashboard.audit.service;

import com.customadmindashboard.audit.dto.AuditLogResponse;
import com.customadmindashboard.audit.entity.AuditLog;
import com.customadmindashboard.audit.repository.AuditLogRepository;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.project.entity.Project;
import com.customadmindashboard.project.repository.ProjectRepository;
import com.customadmindashboard.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;

    // Log save karo — internal use ke liye
    public void log(Tenant tenant, Project project, String action,
                    String tableName, String recordId,
                    String oldValue, String newValue) {
        AuditLog log = AuditLog.builder()
                .tenant(tenant)
                .project(project)
                .action(action)
                .tableName(tableName)
                .recordId(recordId)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        auditLogRepository.save(log);
    }

    // Project ke saare logs dekho
    public List<AuditLogResponse> getProjectLogs(Long projectId, Tenant tenant) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        return auditLogRepository.findTop20ByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Table specific logs
    public List<AuditLogResponse> getTableLogs(Long projectId, String tableName, Tenant tenant) {
        return auditLogRepository
                .findAllByProjectIdAndTableNameOrderByCreatedAtDesc(projectId, tableName)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .tenantId(log.getTenant().getId())
                .projectId(log.getProject().getId())
                .action(log.getAction())
                .tableName(log.getTableName())
                .recordId(log.getRecordId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}