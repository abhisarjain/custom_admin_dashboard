package com.customadmindashboard.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardTableSelectionDto {
    private Long projectId;
    private String tableName;
}
