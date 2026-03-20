package com.customadmindashboard.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private boolean canGrantView = false;
    private boolean canGrantCreate = false;
    private boolean canGrantEdit = false;
    private boolean canGrantDelete = false;
    private boolean canGrantDelegate = false;
}