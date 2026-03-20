package com.customadmindashboard.dashboard.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardViewResponse {
    private Long id;
    private Long projectId;
    private Long dbConnectionId;
    private String tableName;
    private boolean isVisible;
    private boolean isCreatable;
    private boolean isDeletable;
    private List<DashboardColumnResponse> columns;
    private LocalDateTime createdAt;
}