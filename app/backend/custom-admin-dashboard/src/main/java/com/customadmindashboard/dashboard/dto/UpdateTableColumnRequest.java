package com.customadmindashboard.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTableColumnRequest {

    @NotBlank(message = "Column name is required")
    private String columnName;

    @NotBlank(message = "New column name is required")
    private String newColumnName;

    @NotBlank(message = "Data type is required")
    private String dataType;
}
