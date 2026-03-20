package com.customadmindashboard.rbac.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_member_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleMemberPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnore
    private Role role;

    @Column(name = "can_invite", nullable = false, columnDefinition = "boolean default false")
    private boolean canInvite = false;

    @Column(name = "can_view", nullable = false, columnDefinition = "boolean default false")
    private boolean canView = false;

    @Column(name = "can_edit", nullable = false, columnDefinition = "boolean default false")
    private boolean canEdit = false;

    @Column(name = "can_remove", nullable = false, columnDefinition = "boolean default false")
    private boolean canRemove = false;

    @Column(name = "grant_invite", nullable = false, columnDefinition = "boolean default false")
    private boolean grantInvite = false;

    @Column(name = "grant_view", nullable = false, columnDefinition = "boolean default false")
    private boolean grantView = false;

    @Column(name = "grant_edit", nullable = false, columnDefinition = "boolean default false")
    private boolean grantEdit = false;

    @Column(name = "grant_remove", nullable = false, columnDefinition = "boolean default false")
    private boolean grantRemove = false;

    @Column(name = "grant_delegate", nullable = false, columnDefinition = "boolean default false")
    private boolean grantDelegate = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
