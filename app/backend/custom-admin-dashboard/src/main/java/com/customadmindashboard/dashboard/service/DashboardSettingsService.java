package com.customadmindashboard.dashboard.service;

import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.dashboard.dto.DashboardSettingsDto;
import com.customadmindashboard.dashboard.dto.DashboardTableSelectionDto;
import com.customadmindashboard.dashboard.entity.DashboardSettings;
import com.customadmindashboard.dashboard.repository.DashboardSettingsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardSettingsService {

    private static final List<String> ALLOWED_CARD_IDS = List.of("recentActivity", "projects", "tables", "quickActions", "roles", "members");
    private static final int MAX_PROJECT_CARD_ITEMS = 3;
    private static final int MAX_TABLE_ITEMS = 3;
    private static final int MAX_ROLE_PROJECTS = 3;
    private static final int MAX_MEMBER_PROJECTS = 3;

    private final DashboardSettingsRepository dashboardSettingsRepository;
    private final ObjectMapper objectMapper;

    public DashboardSettingsDto getSettings(Tenant tenant) {
        try {
            return dashboardSettingsRepository.findByTenantId(tenant.getId())
                    .map(DashboardSettings::getSettingsJson)
                    .map(this::deserialize)
                    .map(this::sanitize)
                    .orElseGet(this::defaultSettings);
        } catch (RuntimeException ex) {
            return defaultSettings();
        }
    }

    @Transactional
    public DashboardSettingsDto saveSettings(Tenant tenant, DashboardSettingsDto request) {
        DashboardSettingsDto sanitized = sanitize(request);

        DashboardSettings settings = dashboardSettingsRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> DashboardSettings.builder()
                        .tenant(tenant)
                        .settingsJson("{}")
                        .build());

        settings.setSettingsJson(serialize(sanitized));
        dashboardSettingsRepository.save(settings);
        return sanitized;
    }

    private DashboardSettingsDto defaultSettings() {
        return DashboardSettingsDto.builder()
                .showRecentActivity(true)
                .cardOrder(new ArrayList<>(ALLOWED_CARD_IDS))
                .projectCardProjectIds(new ArrayList<>())
                .tableSelections(new ArrayList<>())
                .quickActionIds(new ArrayList<>())
                .roleProjectIds(new ArrayList<>())
                .memberProjectIds(new ArrayList<>())
                .build();
    }

    private DashboardSettingsDto sanitize(DashboardSettingsDto request) {
        DashboardSettingsDto source = request == null ? defaultSettings() : request;

        List<String> requestedOrder = source.getCardOrder() == null ? List.of() : source.getCardOrder();
        LinkedHashSet<String> normalizedOrder = new LinkedHashSet<>();
        requestedOrder.stream()
                .filter(ALLOWED_CARD_IDS::contains)
                .forEach(normalizedOrder::add);
        ALLOWED_CARD_IDS.forEach(normalizedOrder::add);

        return DashboardSettingsDto.builder()
                .showRecentActivity(!Boolean.FALSE.equals(source.getShowRecentActivity()))
                .cardOrder(new ArrayList<>(normalizedOrder))
                .projectCardProjectIds(limitUniqueIds(source.getProjectCardProjectIds(), MAX_PROJECT_CARD_ITEMS))
                .tableSelections(limitUniqueTables(source.getTableSelections(), MAX_TABLE_ITEMS))
                .quickActionIds(limitUniqueStrings(source.getQuickActionIds()))
                .roleProjectIds(limitUniqueIds(source.getRoleProjectIds(), MAX_ROLE_PROJECTS))
                .memberProjectIds(limitUniqueIds(source.getMemberProjectIds(), MAX_MEMBER_PROJECTS))
                .build();
    }

    private List<Long> limitUniqueIds(List<Long> values, int maxItems) {
        if (values == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        values.stream()
                .filter(Objects::nonNull)
                .forEach(unique::add);
        return unique.stream().limit(maxItems).toList();
    }

    private List<String> limitUniqueStrings(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(unique::add);
        return new ArrayList<>(unique);
    }

    private List<DashboardTableSelectionDto> limitUniqueTables(List<DashboardTableSelectionDto> values, int maxItems) {
        if (values == null) {
            return new ArrayList<>();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<DashboardTableSelectionDto> results = new ArrayList<>();

        for (DashboardTableSelectionDto value : values) {
            if (value == null || value.getProjectId() == null || value.getTableName() == null) {
                continue;
            }
            String tableName = value.getTableName().trim();
            if (tableName.isBlank()) {
                continue;
            }
            String key = value.getProjectId() + "::" + tableName;
            if (seen.add(key)) {
                results.add(DashboardTableSelectionDto.builder()
                        .projectId(value.getProjectId())
                        .tableName(tableName)
                        .build());
            }
            if (results.size() >= maxItems) {
                break;
            }
        }

        return results;
    }

    private DashboardSettingsDto deserialize(String json) {
        try {
            return objectMapper.readValue(json, DashboardSettingsDto.class);
        } catch (JsonProcessingException ex) {
            return defaultSettings();
        }
    }

    private String serialize(DashboardSettingsDto settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
