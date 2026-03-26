package com.customadmindashboard.audit.event;

public record AuditLogEvent(
        Long tenantId,
        Long projectId,
        String action,
        String tableName,
        String recordId,
        String oldValue,
        String newValue
) {
}
