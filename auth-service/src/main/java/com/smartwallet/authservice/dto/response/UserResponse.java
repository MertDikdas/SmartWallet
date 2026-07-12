package com.smartwallet.authservice.dto.response;

import com.smartwallet.authservice.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt
) {
}
