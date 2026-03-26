package com.customadmindashboard.audit.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private Long tenantId;
    private String actorName;
    private String actorEmail;
    private Long projectId;
    private String action;
    private String tableName;
    private String recordId;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
