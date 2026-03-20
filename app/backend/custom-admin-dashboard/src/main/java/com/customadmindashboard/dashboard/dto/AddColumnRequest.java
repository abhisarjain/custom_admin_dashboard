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
public class AddColumnRequest {

    @NotBlank(message = "Column name is required")
    private String columnName;

    @NotBlank(message = "Data type is required")
    private String dataType;
}
