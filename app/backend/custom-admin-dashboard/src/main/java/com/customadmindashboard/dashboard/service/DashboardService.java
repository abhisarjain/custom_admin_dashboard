package com.customadmindashboard.dashboard.service;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.common.exception.BadRequestException;
import com.customadmindashboard.common.exception.ResourceNotFoundException;
import com.customadmindashboard.dashboard.dto.*;
import com.customadmindashboard.dashboard.entity.DashboardColumn;
import com.customadmindashboard.dashboard.entity.DashboardView;
import com.customadmindashboard.dashboard.repository.DashboardColumnRepository;
import com.customadmindashboard.dashboard.repository.DashboardViewRepository;
import com.customadmindashboard.database.entity.DbConnection;
import com.customadmindashboard.database.entity.DbSchema;
import com.customadmindashboard.database.repository.DbConnectionRepository;
import com.customadmindashboard.database.repository.DbSchemaRepository;
import com.customadmindashboard.project.repository.ProjectMemberRepository;
import com.customadmindashboard.project.entity.Project;
import com.customadmindashboard.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardViewRepository dashboardViewRepository;
    private final DashboardColumnRepository dashboardColumnRepository;
    private final DbConnectionRepository dbConnectionRepository;
    private final DbSchemaRepository dbSchemaRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // Dashboard view banao — table add karo dashboard mein
    @Transactional
    public DashboardViewResponse createView(Long projectId,
            CreateDashboardViewRequest request, Tenant tenant) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isOwner = project.getTenant().getId().equals(tenant.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndTenantId(projectId, tenant.getId());

        if (!isOwner && !isMember) {
            throw new BadRequestException("Access denied");
        }

        // Already exists?
        if (dashboardViewRepository.existsByProjectIdAndTableName(
                projectId, request.getTableName())) {
            throw new BadRequestException("Table already added to dashboard");
        }

        DbConnection dbConnection = dbConnectionRepository.findById(request.getDbConnectionId())
                .orElseThrow(() -> new ResourceNotFoundException("DB Connection not found"));

        DashboardView view = DashboardView.builder()
                .project(project)
                .dbConnection(dbConnection)
                .tableName(request.getTableName())
                .isVisible(request.isVisible())
                .isCreatable(request.isCreatable())
                .isDeletable(request.isDeletable())
                .build();

        view = dashboardViewRepository.save(view);

        // Schema se automatically columns add karo
        List<DbSchema> schemas = dbSchemaRepository
                .findAllByDbConnectionIdAndTableName(
                        request.getDbConnectionId(), request.getTableName());

        AtomicInteger order = new AtomicInteger(1);
        DashboardView finalView = view;
        List<DashboardColumn> columns = schemas.stream()
                .map(schema -> DashboardColumn.builder()
                        .dashboardView(finalView)
                        .columnName(schema.getColumnName())
                        .isVisible(true)
                        .isEditable(!schema.isPrimaryKey())
                        .isDeletable(false)
                        .displayOrder(order.getAndIncrement())
                        .build())
                .collect(Collectors.toList());

        dashboardColumnRepository.saveAll(columns);

        return mapToResponse(view, columns);
    }

    // Saare views dekho
    public List<DashboardViewResponse> getViews(Long projectId, Tenant tenant) {
    // Owner check hatao — sirf project exist check karo
    projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

    return dashboardViewRepository.findAllByProjectId(projectId)
            .stream()
            .map(view -> {
                List<DashboardColumn> columns = dashboardColumnRepository
                        .findAllByDashboardViewIdOrderByDisplayOrder(view.getId());
                return mapToResponse(view, columns);
            })
            .collect(Collectors.toList());
}

    // Column update karo
    @Transactional
    public DashboardColumnResponse updateColumn(Long viewId,
            UpdateDashboardColumnRequest request, Tenant tenant) {

        DashboardView view = dashboardViewRepository.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard view not found"));

        DashboardColumn column = dashboardColumnRepository
                .findByDashboardViewIdAndColumnName(viewId, request.getColumnName())
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

        column.setVisible(request.isVisible());
        column.setEditable(request.isEditable());
        column.setDeletable(request.isDeletable());
        column.setDisplayOrder(request.getDisplayOrder());

        column = dashboardColumnRepository.save(column);

        return mapColumnToResponse(column);
    }

    private DashboardViewResponse mapToResponse(DashboardView view,
            List<DashboardColumn> columns) {
        return DashboardViewResponse.builder()
                .id(view.getId())
                .projectId(view.getProject().getId())
                .dbConnectionId(view.getDbConnection().getId())
                .tableName(view.getTableName())
                .isVisible(view.isVisible())
                .isCreatable(view.isCreatable())
                .isDeletable(view.isDeletable())
                .columns(columns.stream()
                        .map(this::mapColumnToResponse)
                        .collect(Collectors.toList()))
                .createdAt(view.getCreatedAt())
                .build();
    }

    private DashboardColumnResponse mapColumnToResponse(DashboardColumn column) {
        return DashboardColumnResponse.builder()
                .id(column.getId())
                .columnName(column.getColumnName())
                .isVisible(column.isVisible())
                .isEditable(column.isEditable())
                .isDeletable(column.isDeletable())
                .displayOrder(column.getDisplayOrder())
                .build();
    }
}
