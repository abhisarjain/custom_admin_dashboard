package com.customadmindashboard.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetPermissionsRequest {

    @NotBlank(message = "Table name is required")
    private String tableName;

    private String columnName; // null = table level

    private boolean canView = false;
    private boolean canViewData = false;
    private boolean canViewStructure = false;
    private boolean canCreate = false;
    private boolean canCreateData = false;
    private boolean canCreateStructure = false;
    private boolean canEdit = false;
    private boolean canEditData = false;
    private boolean canEditStructure = false;
    private boolean canDelete = false;
    private boolean canDeleteData = false;
    private boolean canDeleteStructure = false;
    private boolean canDeleteTable = false;
    private boolean canGrantView = false;
    private boolean canGrantCreate = false;
    private boolean canGrantEdit = false;
    private boolean canGrantDelete = false;
    private boolean canGrantDelegate = false;
}
