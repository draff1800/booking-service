package com.draff1800.booking_service.user.api.dto.response;

public record AuthResponse(
    String token,
    UserResponse user
) {}
