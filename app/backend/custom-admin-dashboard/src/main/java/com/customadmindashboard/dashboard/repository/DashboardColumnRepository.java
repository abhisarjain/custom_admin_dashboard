package com.customadmindashboard.dashboard.repository;

import com.customadmindashboard.dashboard.entity.DashboardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardColumnRepository extends JpaRepository<DashboardColumn, Long> {
    List<DashboardColumn> findAllByDashboardViewIdOrderByDisplayOrder(Long dashboardViewId);
    List<DashboardColumn> findAllByDashboardViewIdAndIsVisible(Long dashboardViewId, boolean isVisible);
    Optional<DashboardColumn> findByDashboardViewIdAndColumnName(Long dashboardViewId, String columnName);
    void deleteAllByDashboardViewId(Long dashboardViewId);
}
