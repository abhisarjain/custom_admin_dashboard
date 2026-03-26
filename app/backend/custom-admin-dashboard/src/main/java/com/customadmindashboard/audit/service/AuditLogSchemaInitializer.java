package com.customadmindashboard.audit.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAuditTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id BIGSERIAL PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    project_id BIGINT NOT NULL,
                    action VARCHAR(255) NOT NULL,
                    table_name VARCHAR(255),
                    record_id VARCHAR(255),
                    old_value TEXT,
                    new_value TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_project_id ON audit_logs(project_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at)");
        } catch (Exception ex) {
            log.warn("Audit table initialization skipped: {}", ex.getMessage());
        }
    }
}
