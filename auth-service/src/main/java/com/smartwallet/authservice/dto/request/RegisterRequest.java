package com.smartwallet.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "E-mail is required")
        @Size(max = 255, message = "E-mail cannot exceed 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 64,
                message = "Password must be between 8 and 64 characters"
        )
        String password
) {
}
