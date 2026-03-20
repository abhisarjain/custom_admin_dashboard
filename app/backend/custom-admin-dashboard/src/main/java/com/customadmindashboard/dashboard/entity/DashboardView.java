package com.customadmindashboard.dashboard.entity;

import com.customadmindashboard.database.entity.DbConnection;
import com.customadmindashboard.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "db_connection_id", nullable = false)
    private DbConnection dbConnection;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

    @Column(name = "is_creatable", nullable = false)
    private boolean isCreatable = true;

    @Column(name = "is_deletable", nullable = false)
    private boolean isDeletable = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}