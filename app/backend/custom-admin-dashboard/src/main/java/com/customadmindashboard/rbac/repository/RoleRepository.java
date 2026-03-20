package com.customadmindashboard.rbac.repository;

import com.customadmindashboard.rbac.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findAllByProjectId(Long projectId);
    Optional<Role> findByNameAndProjectId(String name, Long projectId);
    boolean existsByNameAndProjectId(String name, Long projectId);
    void deleteAllByProjectId(Long projectId);
}
