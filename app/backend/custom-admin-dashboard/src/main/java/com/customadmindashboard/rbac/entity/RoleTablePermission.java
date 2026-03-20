package com.customadmindashboard.rbac.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_table_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleTablePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnore
    private Role role;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "can_view", nullable = false, columnDefinition = "boolean default false")
    private boolean canView = false;

    @Column(name = "can_view_data", nullable = false, columnDefinition = "boolean default false")
    private boolean canViewData = false;

    @Column(name = "can_view_structure", nullable = false, columnDefinition = "boolean default false")
    private boolean canViewStructure = false;

    @Column(name = "can_create", nullable = false, columnDefinition = "boolean default false")
    private boolean canCreate = false;

    @Column(name = "can_create_data", nullable = false, columnDefinition = "boolean default false")
    private boolean canCreateData = false;

    @Column(name = "can_create_structure", nullable = false, columnDefinition = "boolean default false")
    private boolean canCreateStructure = false;

    @Column(name = "can_edit", nullable = false, columnDefinition = "boolean default false")
    private boolean canEdit = false;

    @Column(name = "can_edit_data", nullable = false, columnDefinition = "boolean default false")
    private boolean canEditData = false;

    @Column(name = "can_edit_structure", nullable = false, columnDefinition = "boolean default false")
    private boolean canEditStructure = false;

    @Column(name = "can_delete", nullable = false, columnDefinition = "boolean default false")
    private boolean canDelete = false;

    @Column(name = "can_delete_data", nullable = false, columnDefinition = "boolean default false")
    private boolean canDeleteData = false;

    @Column(name = "can_delete_structure", nullable = false, columnDefinition = "boolean default false")
    private boolean canDeleteStructure = false;

    @Column(name = "can_delete_table", nullable = false, columnDefinition = "boolean default false")
    private boolean canDeleteTable = false;

    @Column(name = "can_grant_view", nullable = false, columnDefinition = "boolean default false")
    private boolean canGrantView = false;

    @Column(name = "can_grant_create", nullable = false, columnDefinition = "boolean default false")
    private boolean canGrantCreate = false;

    @Column(name = "can_grant_edit", nullable = false, columnDefinition = "boolean default false")
    private boolean canGrantEdit = false;

    @Column(name = "can_grant_delete", nullable = false, columnDefinition = "boolean default false")
    private boolean canGrantDelete = false;

    @Column(name = "can_grant_delegate", nullable = false, columnDefinition = "boolean default false")
    private boolean canGrantDelegate = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
