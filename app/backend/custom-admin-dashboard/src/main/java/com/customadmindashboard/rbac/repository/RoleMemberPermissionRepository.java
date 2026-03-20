package com.customadmindashboard.rbac.repository;

import com.customadmindashboard.rbac.entity.RoleMemberPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleMemberPermissionRepository extends JpaRepository<RoleMemberPermission, Long> {
    Optional<RoleMemberPermission> findByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
}
