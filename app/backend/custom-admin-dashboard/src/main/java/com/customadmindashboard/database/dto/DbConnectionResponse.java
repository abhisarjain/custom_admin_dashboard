package com.customadmindashboard.database.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbConnectionResponse {
    private Long id;
    private Long projectId;
    private String dbType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private boolean sslEnabled;
    private String status;
    private LocalDateTime lastConnectedAt;
    private LocalDateTime createdAt;
}