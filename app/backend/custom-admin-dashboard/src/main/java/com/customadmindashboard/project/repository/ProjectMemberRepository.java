package com.customadmindashboard.project.repository;

import com.customadmindashboard.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findAllByProjectId(Long projectId);
    List<ProjectMember> findAllByTenantId(Long tenantId);
    Optional<ProjectMember> findByProjectIdAndTenantId(Long projectId, Long tenantId);
    boolean existsByProjectIdAndTenantId(Long projectId, Long tenantId);
    boolean existsByRoleId(Long roleId);
    void deleteAllByProjectId(Long projectId);
}
