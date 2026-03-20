package com.customadmindashboard.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDashboardViewRequest {

    @NotNull(message = "DB Connection ID is required")
    private Long dbConnectionId;

    @NotBlank(message = "Table name is required")
    private String tableName;

    private boolean isVisible = true;
    private boolean isCreatable = true;
    private boolean isDeletable = false;
}