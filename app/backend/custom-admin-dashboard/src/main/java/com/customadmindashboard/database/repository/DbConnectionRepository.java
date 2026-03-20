package com.customadmindashboard.database.repository;

import com.customadmindashboard.database.entity.DbConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DbConnectionRepository extends JpaRepository<DbConnection, Long> {
    List<DbConnection> findAllByProjectId(Long projectId);
    Optional<DbConnection> findByIdAndProjectId(Long id, Long projectId);
    boolean existsByProjectIdAndDatabaseName(Long projectId, String databaseName);
    void deleteAllByProjectId(Long projectId);
}
