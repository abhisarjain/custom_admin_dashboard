package com.customadmindashboard.database.repository;

import com.customadmindashboard.database.entity.DbSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DbSchemaRepository extends JpaRepository<DbSchema, Long> {
    List<DbSchema> findAllByDbConnectionId(Long dbConnectionId);
    List<DbSchema> findAllByDbConnectionIdAndTableName(Long dbConnectionId, String tableName);
    void deleteAllByDbConnectionId(Long dbConnectionId);
}