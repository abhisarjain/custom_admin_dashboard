package com.customadmindashboard.rbac.repository;

import com.customadmindashboard.rbac.entity.MemberPermissionGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberPermissionGrantRepository extends JpaRepository<MemberPermissionGrant, Long> {
    List<MemberPermissionGrant> findAllByProjectIdAndGranteeId(Long projectId, Long granteeId);
    List<MemberPermissionGrant> findAllByProjectIdAndGranteeIdAndTableName(Long projectId, Long granteeId, String tableName);
    Optional<MemberPermissionGrant> findByProjectIdAndGranteeIdAndTableNameAndColumnName(Long projectId, Long granteeId, String tableName, String columnName);
    void deleteAllByProjectId(Long projectId);
}
