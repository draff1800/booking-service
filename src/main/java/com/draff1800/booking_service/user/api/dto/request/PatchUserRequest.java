package com.draff1800.booking_service.user.api.dto.request;

import jakarta.validation.constraints.Size;

public record PatchUserRequest(
    @Size(max = 50, message = "handle must be 50 characters or less")
    String handle,

    @Size(max = 80, message = "displayName must be 80 characters or less")
    String displayName
) {}
