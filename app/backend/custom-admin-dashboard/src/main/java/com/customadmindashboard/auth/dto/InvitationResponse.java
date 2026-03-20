package com.customadmindashboard.auth.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private String invitedByName;
    private String roleName;
    private String email;
    private String token;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}