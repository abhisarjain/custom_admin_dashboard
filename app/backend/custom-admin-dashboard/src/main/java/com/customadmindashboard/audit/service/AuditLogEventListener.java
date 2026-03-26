package com.customadmindashboard.audit.service;

import com.customadmindashboard.audit.event.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogEventListener {

    private final AuditLogWriter auditLogWriter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(AuditLogEvent event) {
        try {
            auditLogWriter.write(event);
        } catch (Exception ex) {
            log.warn("Audit log skipped for action {}: {}", event.action(), ex.getMessage());
        }
    }
}
