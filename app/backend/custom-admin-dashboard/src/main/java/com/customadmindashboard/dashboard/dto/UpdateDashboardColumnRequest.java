package com.customadmindashboard.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDashboardColumnRequest {

    @NotBlank(message = "Column name is required")
    private String columnName;

    private boolean isVisible = true;
    private boolean isEditable = false;
    private boolean isDeletable = false;
    private Integer displayOrder = 0;
}