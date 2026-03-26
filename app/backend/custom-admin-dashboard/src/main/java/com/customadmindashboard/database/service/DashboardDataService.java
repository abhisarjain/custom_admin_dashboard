package com.customadmindashboard.database.service;

import com.customadmindashboard.audit.service.AuditService;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.exception.BadRequestException;
import com.customadmindashboard.common.exception.ResourceNotFoundException;
import com.customadmindashboard.dashboard.dto.AddColumnRequest;
import com.customadmindashboard.dashboard.dto.CreateTableRequest;
import com.customadmindashboard.dashboard.dto.UpdateTableColumnRequest;
import com.customadmindashboard.dashboard.entity.DashboardColumn;
import com.customadmindashboard.dashboard.entity.DashboardView;
import com.customadmindashboard.dashboard.repository.DashboardColumnRepository;
import com.customadmindashboard.dashboard.repository.DashboardViewRepository;
import com.customadmindashboard.database.entity.DbConnection;
import com.customadmindashboard.database.entity.DbSchema;
import com.customadmindashboard.database.repository.DbConnectionRepository;
import com.customadmindashboard.database.repository.DbSchemaRepository;
import com.customadmindashboard.project.entity.ProjectMember;
import com.customadmindashboard.project.entity.Project;
import com.customadmindashboard.project.repository.ProjectRepository;
import com.customadmindashboard.project.repository.ProjectMemberRepository;
import com.customadmindashboard.rbac.entity.RoleTablePermission;
import com.customadmindashboard.rbac.repository.RoleRepository;
import com.customadmindashboard.rbac.repository.RoleTablePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardDataService {

    private final DashboardViewRepository dashboardViewRepository;
    private final DashboardColumnRepository dashboardColumnRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RoleTablePermissionRepository roleTablePermissionRepository;
    private final DbConnectionRepository dbConnectionRepository;
    private final DbSchemaRepository dbSchemaRepository;
    private final ProjectRepository projectRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    // Rows fetch karo
    public List<Map<String, Object>> getRows(Long viewId, Tenant tenant) {
        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));
        RoleTablePermission permission = getTablePermission(view, tenant);

        if (!canViewData(permission)) {
            throw new BadRequestException("View not allowed for this table");
        }

        DbConnection conn = view.getDbConnection();
        String primaryKeyColumn = getPrimaryKeyColumn(view);
        List<DashboardColumn> columns = dashboardColumnRepository
                .findAllByDashboardViewIdOrderByDisplayOrder(viewId)
                .stream()
                .filter(DashboardColumn::isVisible)
                .toList();

        String columnNames = columns.stream()
                .map(DashboardColumn::getColumnName)
                .filter(columnName -> !columnName.equalsIgnoreCase(primaryKeyColumn))
                .reduce((a, b) -> a + ", " + b)
                .map(names -> primaryKeyColumn + ", " + names)
                .orElse(primaryKeyColumn);

        String sql = String.format("SELECT %s FROM %s LIMIT 100", columnNames, view.getTableName());

        return executeQuery(conn, sql);
    }

    // Row create karo
    public void createRow(Long viewId, Map<String, Object> data, Tenant tenant) {
        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));
        RoleTablePermission permission = getTablePermission(view, tenant);

        if (!permission.isCanCreateData()) {
            throw new BadRequestException("Create not allowed for this view");
        }

        DbConnection conn = view.getDbConnection();

        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream().map(k -> "?").reduce((a, b) -> a + ", " + b).orElse("");
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", view.getTableName(), columns, placeholders);

        executeUpdate(conn, sql, new ArrayList<>(data.values()));
        auditService.publish(
                tenant,
                view.getProject(),
                "ROW_CREATED",
                view.getTableName(),
                null,
                null,
                data
        );
    }

    // Row update karo
    public void updateRow(Long viewId, Object rowId, Map<String, Object> data, Tenant tenant) {
        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));
        RoleTablePermission permission = getTablePermission(view, tenant);

        if (!permission.isCanEditData()) {
            throw new BadRequestException("Edit not allowed for this table");
        }

        DbConnection conn = view.getDbConnection();
        String primaryKeyColumn = getPrimaryKeyColumn(view);
        Map<String, Object> oldRow = fetchSingleRow(view, primaryKeyColumn, rowId);

        data.remove("id");
        data.remove(primaryKeyColumn);
        String setClause = data.keySet().stream()
                .map(k -> k + " = ?")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        String sql = String.format("UPDATE %s SET %s WHERE %s = ?", view.getTableName(), setClause, primaryKeyColumn);

        List<Object> params = new ArrayList<>(data.values());
        params.add(rowId);

        executeUpdate(conn, sql, params);
        Map<String, Object> newRow = fetchSingleRow(view, primaryKeyColumn, rowId);
        auditService.publish(
                tenant,
                view.getProject(),
                "ROW_UPDATED",
                view.getTableName(),
                String.valueOf(rowId),
                oldRow,
                newRow != null ? newRow : data
        );
    }

    // Row delete karo
    public void deleteRow(Long viewId, Object rowId, Tenant tenant) {
        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));
        RoleTablePermission permission = getTablePermission(view, tenant);

        if (!permission.isCanDeleteData()) {
            throw new BadRequestException("Delete not allowed for this view");
        }

        DbConnection conn = view.getDbConnection();
        String primaryKeyColumn = getPrimaryKeyColumn(view);
        Map<String, Object> oldRow = fetchSingleRow(view, primaryKeyColumn, rowId);
        String sql = String.format("DELETE FROM %s WHERE %s = ?", view.getTableName(), primaryKeyColumn);
        executeUpdate(conn, sql, List.of(rowId));
        auditService.publish(
                tenant,
                view.getProject(),
                "ROW_DELETED",
                view.getTableName(),
                String.valueOf(rowId),
                oldRow,
                null
        );
    }

    @Transactional
    public void createTable(Long projectId, CreateTableRequest request, Tenant tenant) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isOwner = project.getTenant().getId().equals(tenant.getId());
        ProjectMember member = null;
        if (!isOwner) {
            member = projectMemberRepository.findByProjectIdAndTenantId(projectId, tenant.getId())
                    .orElseThrow(() -> new BadRequestException("Access denied"));

            RoleTablePermission createPerm = roleTablePermissionRepository
                    .findByRoleIdAndTableName(member.getRole().getId(), "__new_table__")
                    .orElse(RoleTablePermission.builder().build());

            if (!createPerm.isCanCreate()) {
                throw new BadRequestException("Create table not allowed");
            }
        }

        DbConnection conn = dbConnectionRepository.findAllByProjectId(projectId).stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No database connection found"));

        String sql = switch (conn.getDbType().toLowerCase()) {
            case "postgres" -> String.format("CREATE TABLE %s (id BIGSERIAL PRIMARY KEY)", request.getTableName());
            case "mysql" -> String.format("CREATE TABLE %s (id BIGINT AUTO_INCREMENT PRIMARY KEY)", request.getTableName());
            default -> throw new BadRequestException("Unsupported DB type");
        };

        executeUpdate(conn, sql, List.of());

        syncSingleTableSchema(conn, request.getTableName());

        if (!isOwner && member != null) {
            roleTablePermissionRepository.deleteAllByRoleIdAndTableName(member.getRole().getId(), request.getTableName());

            RoleTablePermission permission = RoleTablePermission.builder()
                    .role(member.getRole())
                    .tableName(request.getTableName())
                    .build();
            permission.setCanView(true);
            permission.setCanViewData(true);
            permission.setCanViewStructure(true);
            permission.setCanCreate(true);
            permission.setCanCreateData(true);
            permission.setCanCreateStructure(true);
            permission.setCanEdit(true);
            permission.setCanEditData(true);
            permission.setCanEditStructure(true);
            permission.setCanDelete(true);
            permission.setCanDeleteData(true);
            permission.setCanDeleteStructure(true);
            permission.setCanDeleteTable(true);
            permission.setCanGrantView(true);
            permission.setCanGrantCreate(true);
            permission.setCanGrantEdit(true);
            permission.setCanGrantDelete(true);
            permission.setCanGrantDelegate(true);

            roleTablePermissionRepository.saveAndFlush(permission);
        }

        auditService.publish(
                tenant,
                project,
                "TABLE_CREATED",
                request.getTableName(),
                null,
                null,
                Map.of("tableName", request.getTableName())
        );
    }

    public void addColumn(Long viewId, AddColumnRequest request, Tenant tenant) {
        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));
        RoleTablePermission permission = getTablePermission(view, tenant);

        if (!permission.isCanEditStructure() && !permission.isCanCreateStructure()) {
            throw new BadRequestException("Structure edit not allowed");
        }

        DbConnection conn = view.getDbConnection();
        String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s", view.getTableName(), request.getColumnName(), request.getDataType());
        executeUpdate(conn, sql, List.of());

        dashboardColumnRepository.save(DashboardColumn.builder()
                .dashboardView(view)
                .columnName(request.getColumnName())
                .isVisible(true)
                .isEditable(true)
                .isDeletable(true)
                .displayOrder(dashboardColumnRepository.findAllByDashboardViewIdOrderByDisplayOrder(viewId).size() + 1)
                .build());
        auditService.publish(
                tenant,
                view.getProject(),
                "COLUMN_ADDED",
                view.getTableName(),
                request.getColumnName(),
                null,
                Map.of(
                        "columnName", request.getColumnName(),
                        "dataType", request.getDataType()
                )
        );
    }

    public void deleteColumn(Long viewId, String columnName, Tenant tenant) {
        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));
        RoleTablePermission permission = getTablePermission(view, tenant);

        if (!permission.isCanDeleteStructure()) {
            throw new BadRequestException("Structure delete not allowed");
        }

        if ("id".equalsIgnoreCase(columnName)) {
            throw new BadRequestException("Primary key column cannot be deleted");
        }

        DbConnection conn = view.getDbConnection();
        String sql = String.format("ALTER TABLE %s DROP COLUMN %s", view.getTableName(), columnName);
        executeUpdate(conn, sql, List.of());

        dashboardColumnRepository.findByDashboardViewIdAndColumnName(viewId, columnName)
                .ifPresent(dashboardColumnRepository::delete);
        auditService.publish(
                tenant,
                view.getProject(),
                "COLUMN_DELETED",
                view.getTableName(),
                columnName,
                Map.of("columnName", columnName),
                null
        );
    }

    public void updateColumn(Long viewId, UpdateTableColumnRequest request, Tenant tenant) {
        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));
        RoleTablePermission permission = getTablePermission(view, tenant);

        if (!permission.isCanEditStructure()) {
            throw new BadRequestException("Structure edit not allowed");
        }

        if ("id".equalsIgnoreCase(request.getColumnName())) {
            throw new BadRequestException("Primary key column cannot be edited");
        }

        DashboardColumn column = dashboardColumnRepository
                .findByDashboardViewIdAndColumnName(viewId, request.getColumnName())
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

        DbConnection conn = view.getDbConnection();

        if (!request.getColumnName().equalsIgnoreCase(request.getNewColumnName())) {
            String renameSql = String.format(
                    "ALTER TABLE %s RENAME COLUMN %s TO %s",
                    view.getTableName(),
                    request.getColumnName(),
                    request.getNewColumnName()
            );
            executeUpdate(conn, renameSql, List.of());
        }

        String alterTypeSql = switch (conn.getDbType().toLowerCase()) {
            case "postgres" -> String.format(
                    "ALTER TABLE %s ALTER COLUMN %s TYPE %s",
                    view.getTableName(),
                    request.getNewColumnName(),
                    request.getDataType()
            );
            case "mysql" -> String.format(
                    "ALTER TABLE %s MODIFY COLUMN %s %s",
                    view.getTableName(),
                    request.getNewColumnName(),
                    request.getDataType()
            );
            default -> throw new BadRequestException("Unsupported DB type");
        };
        executeUpdate(conn, alterTypeSql, List.of());

        column.setColumnName(request.getNewColumnName());
        dashboardColumnRepository.save(column);
        auditService.publish(
                tenant,
                view.getProject(),
                "COLUMN_UPDATED",
                view.getTableName(),
                request.getColumnName(),
                Map.of(
                        "columnName", request.getColumnName()
                ),
                Map.of(
                        "columnName", request.getNewColumnName(),
                        "dataType", request.getDataType()
                )
        );
    }

    @Transactional
    public void deleteTable(Long viewId, Tenant tenant) {
        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));
        RoleTablePermission permission = getTablePermission(view, tenant);

        if (!permission.isCanDeleteTable()) {
            throw new BadRequestException("Table delete not allowed");
        }

        DbConnection conn = view.getDbConnection();
        String sql = String.format("DROP TABLE %s", view.getTableName());
        executeUpdate(conn, sql, List.of());
        dbSchemaRepository.deleteAllByDbConnectionIdAndTableName(conn.getId(), view.getTableName());
        roleRepository.findAllByProjectId(view.getProject().getId()).forEach(role ->
                roleTablePermissionRepository.deleteAllByRoleIdAndTableName(role.getId(), view.getTableName())
        );

        dashboardColumnRepository.deleteAllByDashboardViewId(viewId);
        dashboardViewRepository.delete(view);
        auditService.publish(
                tenant,
                view.getProject(),
                "TABLE_DELETED",
                view.getTableName(),
                String.valueOf(viewId),
                Map.of("tableName", view.getTableName()),
                null
        );
    }

    // ---- Helpers ----

    private RoleTablePermission getTablePermission(DashboardView view, Tenant tenant) {
        if (view.getProject().getTenant().getId().equals(tenant.getId())) {
            return RoleTablePermission.builder()
                    .canView(true)
                    .canViewData(true)
                    .canViewStructure(true)
                    .canCreate(true)
                    .canCreateData(true)
                    .canCreateStructure(true)
                    .canEdit(true)
                    .canEditData(true)
                    .canEditStructure(true)
                    .canDelete(true)
                    .canDeleteData(true)
                    .canDeleteStructure(true)
                    .canDeleteTable(true)
                    .canGrantView(true)
                    .canGrantCreate(true)
                    .canGrantEdit(true)
                    .canGrantDelete(true)
                    .canGrantDelegate(true)
                    .build();
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndTenantId(view.getProject().getId(), tenant.getId())
                .orElseThrow(() -> new BadRequestException("Access denied"));

        return roleTablePermissionRepository.findAllByRoleIdAndTableName(member.getRole().getId(), view.getTableName())
                .stream()
                .findFirst()
                .orElse(RoleTablePermission.builder().tableName(view.getTableName()).build());
    }

    private boolean canViewData(RoleTablePermission permission) {
        return permission.isCanViewData()
                || permission.isCanCreateData()
                || permission.isCanEditData()
                || permission.isCanDeleteData();
    }

    private String getPrimaryKeyColumn(DashboardView view) {
        return dbSchemaRepository.findAllByDbConnectionIdAndTableName(view.getDbConnection().getId(), view.getTableName())
                .stream()
                .filter(DbSchema::isPrimaryKey)
                .map(DbSchema::getColumnName)
                .findFirst()
                .orElse("id");
    }

    private void syncSingleTableSchema(DbConnection conn, String tableName) {
        dbSchemaRepository.deleteAllByDbConnectionIdAndTableName(conn.getId(), tableName);
        dbSchemaRepository.saveAllAndFlush(fetchSingleTableSchema(conn, tableName));
    }

    private List<DbSchema> fetchSingleTableSchema(DbConnection conn, String tableName) {
        List<DbSchema> schemas = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(
                buildJdbcUrl(conn), conn.getUsername(), conn.getPassword())) {

            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = null;
            String schemaPattern = null;

            if ("mysql".equalsIgnoreCase(conn.getDbType())) {
                catalog = conn.getDatabaseName();
            } else if ("postgres".equalsIgnoreCase(conn.getDbType())) {
                schemaPattern = "public";
            }

            Set<String> primaryKeys = new HashSet<>();
            ResultSet pks = metaData.getPrimaryKeys(catalog, schemaPattern, tableName);
            while (pks.next()) {
                primaryKeys.add(pks.getString("COLUMN_NAME"));
            }

            Map<String, String> foreignKeys = new HashMap<>();
            ResultSet fks = metaData.getImportedKeys(catalog, schemaPattern, tableName);
            while (fks.next()) {
                foreignKeys.put(fks.getString("FKCOLUMN_NAME"), fks.getString("PKTABLE_NAME"));
            }

            ResultSet columns = metaData.getColumns(catalog, schemaPattern, tableName, "%");
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                schemas.add(DbSchema.builder()
                        .dbConnection(conn)
                        .tableName(tableName)
                        .columnName(columnName)
                        .dataType(columns.getString("TYPE_NAME"))
                        .isNullable(columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                        .isPrimaryKey(primaryKeys.contains(columnName))
                        .isForeignKey(foreignKeys.containsKey(columnName))
                        .referencedTable(foreignKeys.get(columnName))
                        .build());
            }
        } catch (Exception e) {
            log.error("Single table schema sync failed: {}", e.getMessage());
            throw new BadRequestException("Failed to sync table schema: " + e.getMessage());
        }

        return schemas;
    }

    private String buildJdbcUrl(DbConnection conn) {
        return switch (conn.getDbType().toLowerCase()) {
            case "postgres" -> String.format("jdbc:postgresql://%s:%d/%s",
                    conn.getHost(), conn.getPort(), conn.getDatabaseName());
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s",
                    conn.getHost(), conn.getPort(), conn.getDatabaseName());
            default -> throw new BadRequestException("Unsupported DB type");
        };
    }

    private Map<String, Object> fetchSingleRow(DashboardView view, String primaryKeyColumn, Object rowId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ? LIMIT 1", view.getTableName(), primaryKeyColumn);
        List<Map<String, Object>> rows = executeQuery(view.getDbConnection(), sql, List.of(rowId));
        return rows.stream().findFirst().orElse(null);
    }

    private List<Map<String, Object>> executeQuery(DbConnection conn, String sql) {
        return executeQuery(conn, sql, List.of());
    }

    private List<Map<String, Object>> executeQuery(DbConnection conn, String sql, List<Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                buildJdbcUrl(conn), conn.getUsername(), conn.getPassword());
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (Exception e) {
            log.error("Query failed: {}", e.getMessage());
            throw new BadRequestException("Query failed: " + e.getMessage());
        }
        return results;
    }

    private void executeUpdate(DbConnection conn, String sql, List<Object> params) {
        try (Connection connection = DriverManager.getConnection(
                buildJdbcUrl(conn), conn.getUsername(), conn.getPassword());
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error("Update failed: {}", e.getMessage());
            throw new BadRequestException("Update failed: " + e.getMessage());
        }
    }
}
