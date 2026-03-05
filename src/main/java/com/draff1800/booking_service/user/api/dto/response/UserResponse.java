package com.draff1800.booking_service.user.api.dto.response;

public record UserResponse(
    String id,
    String email,
    String role
) {}
