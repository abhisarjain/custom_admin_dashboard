package com.customadmindashboard.rbac.repository;

import com.customadmindashboard.rbac.entity.RoleTablePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleTablePermissionRepository extends JpaRepository<RoleTablePermission, Long> {
    List<RoleTablePermission> findAllByRoleId(Long roleId);
    List<RoleTablePermission> findAllByRoleIdAndTableName(Long roleId, String tableName);
    Optional<RoleTablePermission> findByRoleIdAndTableName(Long roleId, String tableName);
    void deleteByRoleId(Long roleId);
    void deleteAllByRole_ProjectIdAndTableName(Long projectId, String tableName);
    void deleteAllByRoleIdAndTableName(Long roleId, String tableName);
}
