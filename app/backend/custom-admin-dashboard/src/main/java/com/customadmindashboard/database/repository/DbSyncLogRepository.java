package com.customadmindashboard.database.repository;

import com.customadmindashboard.database.entity.DbSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DbSyncLogRepository extends JpaRepository<DbSyncLog, Long> {
    List<DbSyncLog> findAllByDbConnectionIdOrderBySyncedAtDesc(Long dbConnectionId);
    List<DbSyncLog> findTop5ByDbConnectionIdOrderBySyncedAtDesc(Long dbConnectionId);
    void deleteAllByDbConnectionId(Long dbConnectionId);
}
