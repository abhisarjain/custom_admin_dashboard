package com.customadmindashboard.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardColumnResponse {
    private Long id;
    private String columnName;
    private boolean isVisible;
    private boolean isEditable;
    private boolean isDeletable;
    private Integer displayOrder;
}