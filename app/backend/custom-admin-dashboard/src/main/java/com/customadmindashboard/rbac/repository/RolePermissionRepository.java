package com.customadmindashboard.rbac.repository;

import com.customadmindashboard.rbac.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    Optional<RolePermission> findByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
}
