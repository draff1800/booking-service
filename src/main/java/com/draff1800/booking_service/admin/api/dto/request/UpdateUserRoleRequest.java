package com.draff1800.booking_service.admin.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(
    @NotBlank String role
) {}
