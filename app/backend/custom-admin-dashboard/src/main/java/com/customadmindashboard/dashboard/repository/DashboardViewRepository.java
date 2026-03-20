package com.customadmindashboard.dashboard.repository;

import com.customadmindashboard.dashboard.entity.DashboardView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardViewRepository extends JpaRepository<DashboardView, Long> {
    List<DashboardView> findAllByProjectId(Long projectId);
    List<DashboardView> findAllByProjectIdAndIsVisible(Long projectId, boolean isVisible);
    Optional<DashboardView> findByProjectIdAndTableName(Long projectId, String tableName);
    boolean existsByProjectIdAndTableName(Long projectId, String tableName);
    void deleteAllByProjectId(Long projectId);
}
