package com.customadmindashboard.auth.service;

import com.customadmindashboard.auth.dto.AuthResponse;
import com.customadmindashboard.auth.dto.LoginRequest;
import com.customadmindashboard.auth.dto.RegisterRequest;
import com.customadmindashboard.auth.entity.Tenant;
import com.customadmindashboard.auth.repository.TenantRepository;
import com.customadmindashboard.common.exception.BadRequestException;
import com.customadmindashboard.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Register
    public AuthResponse register(RegisterRequest request) {

        // Email already exists?
        if (tenantRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Tenant banao
        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .plan("free")
                .build();

        tenant = tenantRepository.save(tenant);

        // Token generate karo
        String token = jwtUtil.generateToken(tenant.getId(), tenant.getEmail());

        return AuthResponse.builder()
                .tenantId(tenant.getId())
                .name(tenant.getName())
                .email(tenant.getEmail())
                .token(token)
                .build();
    }

    // Login
    public AuthResponse login(LoginRequest request) {

        // Tenant dhundo
        Tenant tenant = tenantRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Password check karo
        if (!passwordEncoder.matches(request.getPassword(), tenant.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Token generate karo
        String token = jwtUtil.generateToken(tenant.getId(), tenant.getEmail());

        return AuthResponse.builder()
                .tenantId(tenant.getId())
                .name(tenant.getName())
                .email(tenant.getEmail())
                .token(token)
                .build();
    }
}