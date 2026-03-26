package com.customadmindashboard.dashboard.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSettingsDto {
    @Builder.Default
    private Boolean showRecentActivity = true;

    @Builder.Default
    private List<String> cardOrder = new ArrayList<>();

    @Builder.Default
    private List<Long> projectCardProjectIds = new ArrayList<>();

    @Builder.Default
    private List<DashboardTableSelectionDto> tableSelections = new ArrayList<>();

    @Builder.Default
    private List<String> quickActionIds = new ArrayList<>();

    @Builder.Default
    private List<Long> roleProjectIds = new ArrayList<>();

    @Builder.Default
    private List<Long> memberProjectIds = new ArrayList<>();
}
