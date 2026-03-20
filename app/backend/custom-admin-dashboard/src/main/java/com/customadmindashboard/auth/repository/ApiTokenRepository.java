package com.customadmindashboard.auth.repository;

import com.customadmindashboard.auth.entity.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
    Optional<ApiToken> findByTokenHash(String tokenHash);
    List<ApiToken> findAllByTenantId(Long tenantId);
    List<ApiToken> findAllByProjectId(Long projectId);
    void deleteAllByProjectId(Long projectId);
}
