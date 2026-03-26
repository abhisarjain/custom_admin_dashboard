package com.customadmindashboard.database.service;

import com.customadmindashboard.audit.service.AuditService;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.exception.BadRequestException;
import com.customadmindashboard.common.exception.ResourceNotFoundException;
import com.customadmindashboard.database.dto.CreateDbConnectionRequest;
import com.customadmindashboard.database.dto.DbConnectionResponse;
import com.customadmindashboard.database.dto.DbSchemaResponse;
import com.customadmindashboard.database.entity.DbConnection;
import com.customadmindashboard.database.entity.DbSchema;
import com.customadmindashboard.database.entity.DbSyncLog;
import com.customadmindashboard.database.repository.DbConnectionRepository;
import com.customadmindashboard.database.repository.DbSchemaRepository;
import com.customadmindashboard.database.repository.DbSyncLogRepository;
import com.customadmindashboard.project.entity.Project;
import com.customadmindashboard.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DbConnectionService {

    private final DbConnectionRepository dbConnectionRepository;
    private final DbSchemaRepository dbSchemaRepository;
    private final DbSyncLogRepository dbSyncLogRepository;
    private final ProjectRepository projectRepository;
    private final AuditService auditService;

    // DB Connection save karo
    @Transactional
    public DbConnectionResponse createConnection(Long projectId,
            CreateDbConnectionRequest request, Tenant tenant) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getTenant().getId().equals(tenant.getId())) {
            throw new BadRequestException("Access denied");
        }

        // Test connection pehle
        boolean connected = testConnection(request.getDbType(), request.getHost(),
                request.getPort(), request.getDatabaseName(),
                request.getUsername(), request.getPassword());

        DbConnection connection = DbConnection.builder()
                .project(project)
                .dbType(request.getDbType())
                .host(request.getHost())
                .port(request.getPort())
                .databaseName(request.getDatabaseName())
                .username(request.getUsername())
                .password(request.getPassword()) // jasypt encrypt hoga
                .sslEnabled(request.isSslEnabled())
                .status(connected ? "active" : "failed")
                .lastConnectedAt(connected ? LocalDateTime.now() : null)
                .build();

        connection = dbConnectionRepository.save(connection);

        if (!connected) {
            throw new BadRequestException("Could not connect to database — check credentials");
        }

        Map<String, Object> connectionState = new LinkedHashMap<>();
        connectionState.put("dbType", connection.getDbType());
        connectionState.put("host", connection.getHost());
        connectionState.put("port", connection.getPort());
        connectionState.put("databaseName", connection.getDatabaseName());
        connectionState.put("status", connection.getStatus());
        auditService.publish(
                tenant,
                project,
                "CONNECTION_CREATED",
                null,
                String.valueOf(connection.getId()),
                null,
                connectionState
        );

        return mapToResponse(connection);
    }

    // Schema sync karo
    @Transactional
    public List<DbSchemaResponse> syncSchema(Long connectionId, Tenant tenant) {

        DbConnection connection = dbConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));

        int tablesDetected = 0;
        String errorMessage = null;

        try {
            // Purana schema delete karo
            dbSchemaRepository.deleteAllByDbConnectionId(connectionId);

            // Naya schema fetch karo
            List<DbSchema> schemas = fetchSchema(connection);
            dbSchemaRepository.saveAll(schemas);

            tablesDetected = (int) schemas.stream()
                    .map(DbSchema::getTableName)
                    .distinct()
                    .count();

            // Connection status update karo
            connection.setStatus("active");
            connection.setLastConnectedAt(LocalDateTime.now());
            dbConnectionRepository.save(connection);

        } catch (Exception e) {
            errorMessage = e.getMessage();
            connection.setStatus("failed");
            dbConnectionRepository.save(connection);
            log.error("Schema sync failed: {}", e.getMessage());
        }

        // Sync log save karo
        DbSyncLog syncLog = DbSyncLog.builder()
                .dbConnection(connection)
                .status(errorMessage == null ? "success" : "failed")
                .tablesDetected(tablesDetected)
                .errorMessage(errorMessage)
                .build();
        dbSyncLogRepository.save(syncLog);

        if (errorMessage != null) {
            throw new BadRequestException("Schema sync failed: " + errorMessage);
        }

        Map<String, Object> syncState = new LinkedHashMap<>();
        syncState.put("connectionId", connectionId);
        syncState.put("tablesDetected", tablesDetected);
        syncState.put("status", "success");
        auditService.publish(
                tenant,
                connection.getProject(),
                "SCHEMA_SYNCED",
                null,
                String.valueOf(connectionId),
                null,
                syncState
        );

        // Response banao
        return buildSchemaResponse(connectionId);
    }

    // Schema dekho
    public List<DbSchemaResponse> getSchema(Long connectionId, Tenant tenant) {
        dbConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        return buildSchemaResponse(connectionId);
    }

    // Saare connections dekho
   public List<DbConnectionResponse> getConnections(Long projectId, Tenant tenant) {
    return dbConnectionRepository.findAllByProjectId(projectId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

    // ---- Private helpers ----

    private boolean testConnection(String dbType, String host, Integer port,
            String dbName, String username, String password) {
        try {
            String url = buildJdbcUrl(dbType, host, port, dbName);
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            log.error("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private List<DbSchema> fetchSchema(DbConnection connection) throws Exception {
        List<DbSchema> schemas = new ArrayList<>();
        String url = buildJdbcUrl(connection.getDbType(), connection.getHost(),
                connection.getPort(), connection.getDatabaseName());

        try (Connection conn = DriverManager.getConnection(url,
                connection.getUsername(), connection.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = null;
            String schemaPattern = null;

            if ("mysql".equalsIgnoreCase(connection.getDbType())) {
                catalog = connection.getDatabaseName();
            } else if ("postgres".equalsIgnoreCase(connection.getDbType())) {
                schemaPattern = "public";
            }

            ResultSet tables = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");

                // Primary keys
                Set<String> primaryKeys = new HashSet<>();
                ResultSet pks = metaData.getPrimaryKeys(catalog, schemaPattern, tableName);
                while (pks.next()) {
                    primaryKeys.add(pks.getString("COLUMN_NAME"));
                }

                // Foreign keys
                Map<String, String> foreignKeys = new HashMap<>();
                ResultSet fks = metaData.getImportedKeys(catalog, schemaPattern, tableName);
                while (fks.next()) {
                    foreignKeys.put(fks.getString("FKCOLUMN_NAME"),
                            fks.getString("PKTABLE_NAME"));
                }

                // Columns
                ResultSet columns = metaData.getColumns(catalog, schemaPattern, tableName, "%");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    schemas.add(DbSchema.builder()
                            .dbConnection(connection)
                            .tableName(tableName)
                            .columnName(columnName)
                            .dataType(columns.getString("TYPE_NAME"))
                            .isNullable(columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                            .isPrimaryKey(primaryKeys.contains(columnName))
                            .isForeignKey(foreignKeys.containsKey(columnName))
                            .referencedTable(foreignKeys.get(columnName))
                            .build());
                }
            }
        }
        return schemas;
    }

    private String buildJdbcUrl(String dbType, String host, Integer port, String dbName) {
        return switch (dbType.toLowerCase()) {
            case "postgres" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
            default -> throw new BadRequestException("Unsupported DB type: " + dbType);
        };
    }

    private List<DbSchemaResponse> buildSchemaResponse(Long connectionId) {
        List<DbSchema> schemas = dbSchemaRepository.findAllByDbConnectionId(connectionId);

        Map<String, List<DbSchema>> grouped = schemas.stream()
                .collect(Collectors.groupingBy(DbSchema::getTableName));

        return grouped.entrySet().stream()
                .map(entry -> DbSchemaResponse.builder()
                        .tableName(entry.getKey())
                        .columns(entry.getValue().stream()
                                .map(s -> DbSchemaResponse.ColumnInfo.builder()
                                        .columnName(s.getColumnName())
                                        .dataType(s.getDataType())
                                        .isNullable(s.isNullable())
                                        .isPrimaryKey(s.isPrimaryKey())
                                        .isForeignKey(s.isForeignKey())
                                        .referencedTable(s.getReferencedTable())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    private DbConnectionResponse mapToResponse(DbConnection c) {
        return DbConnectionResponse.builder()
                .id(c.getId())
                .projectId(c.getProject().getId())
                .dbType(c.getDbType())
                .host(c.getHost())
                .port(c.getPort())
                .databaseName(c.getDatabaseName())
                .username(c.getUsername())
                .sslEnabled(c.isSslEnabled())
                .status(c.getStatus())
                .lastConnectedAt(c.getLastConnectedAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
