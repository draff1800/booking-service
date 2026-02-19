package com.draff1800.booking_service.user.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters long")
    String password
) {}
