package com.customadmindashboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInviteRequest {

    @NotBlank(message = "Token is required")
    private String token;
}