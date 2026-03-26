package com.customadmindashboard.dashboard.repository;

import com.customadmindashboard.dashboard.entity.DashboardSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardSettingsRepository extends JpaRepository<DashboardSettings, Long> {
    Optional<DashboardSettings> findByTenantId(Long tenantId);
}
