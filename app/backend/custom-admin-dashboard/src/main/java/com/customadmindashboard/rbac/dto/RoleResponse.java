package com.customadmindashboard.rbac.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
    private Long id;
    private String name;
    private Long projectId;
    private boolean canGrantView;
    private boolean canGrantCreate;
    private boolean canGrantEdit;
    private boolean canGrantDelete;
    private boolean canGrantDelegate;
    private LocalDateTime createdAt;
}