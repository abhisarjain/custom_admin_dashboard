package com.customadmindashboard.audit.service;

import com.customadmindashboard.audit.event.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogWriter {

    private final JdbcTemplate jdbcTemplate;
    private volatile Boolean useJsonPayloadColumns;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditLogEvent event) {
        boolean jsonPayloadColumns = usesJsonPayloadColumns();
        String payloadPlaceholder = jsonPayloadColumns ? "CAST(? AS jsonb)" : "?";

        jdbcTemplate.update(
                """
                INSERT INTO audit_logs (
                    tenant_id,
                    project_id,
                    action,
                    table_name,
                    record_id,
                    old_value,
                    new_value,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, %s, %s, CURRENT_TIMESTAMP)
                """.formatted(payloadPlaceholder, payloadPlaceholder),
                event.tenantId(),
                event.projectId(),
                event.action(),
                event.tableName(),
                event.recordId(),
                event.oldValue(),
                event.newValue()
        );
    }

    private boolean usesJsonPayloadColumns() {
        if (useJsonPayloadColumns != null) {
            return useJsonPayloadColumns;
        }

        try {
            useJsonPayloadColumns = jdbcTemplate.query(
                    """
                    SELECT data_type, udt_name
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'audit_logs'
                      AND column_name = 'old_value'
                    LIMIT 1
                    """,
                    rs -> {
                        if (!rs.next()) {
                            return false;
                        }
                        String dataType = rs.getString("data_type");
                        String udtName = rs.getString("udt_name");
                        String normalized = ((dataType == null ? "" : dataType) + " " + (udtName == null ? "" : udtName))
                                .toLowerCase();
                        return normalized.contains("json");
                    }
            );
        } catch (Exception ex) {
            useJsonPayloadColumns = false;
        }

        return useJsonPayloadColumns;
    }
}
