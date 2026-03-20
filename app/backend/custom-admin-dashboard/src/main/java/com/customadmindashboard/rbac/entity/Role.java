package com.customadmindashboard.rbac.entity;

import com.customadmindashboard.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(name = "can_grant_view", nullable = false)
    private boolean canGrantView = false;

    @Column(name = "can_grant_create", nullable = false)
    private boolean canGrantCreate = false;

    @Column(name = "can_grant_edit", nullable = false)
    private boolean canGrantEdit = false;

    @Column(name = "can_grant_delete", nullable = false)
    private boolean canGrantDelete = false;

    @Column(name = "can_grant_delegate", nullable = false)
    private boolean canGrantDelegate = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}