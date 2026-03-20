package com.customadmindashboard.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long tenantId;
    private String name;
    private String email;
    private String token;
}