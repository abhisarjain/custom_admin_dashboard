package com.customadmindashboard.rbac.repository;

import com.customadmindashboard.rbac.entity.RoleColumnPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleColumnPermissionRepository extends JpaRepository<RoleColumnPermission, Long> {
    List<RoleColumnPermission> findAllByRoleId(Long roleId);
    List<RoleColumnPermission> findAllByRoleIdAndTableName(Long roleId, String tableName);
    Optional<RoleColumnPermission> findByRoleIdAndTableNameAndColumnName(Long roleId, String tableName, String columnName);
    void deleteByRoleId(Long roleId);
}
