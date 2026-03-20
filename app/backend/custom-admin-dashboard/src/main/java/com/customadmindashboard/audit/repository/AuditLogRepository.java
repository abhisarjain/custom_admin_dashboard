package com.customadmindashboard.audit.repository;

import com.customadmindashboard.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<AuditLog> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<AuditLog> findAllByProjectIdAndTableNameOrderByCreatedAtDesc(Long projectId, String tableName);
    List<AuditLog> findTop20ByProjectIdOrderByCreatedAtDesc(Long projectId);
    void deleteAllByProjectId(Long projectId);
}
