package com.customadmindashboard.rbac.entity;

import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_permission_grants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPermissionGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granter_id", nullable = false)
    private Tenant granter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_id", nullable = false)
    private Tenant grantee;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "column_name")
    private String columnName; // null means table level grant

    @Column(name = "can_view", nullable = false)
    private boolean canView = false;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = false;

    @Column(name = "can_edit", nullable = false)
    private boolean canEdit = false;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}