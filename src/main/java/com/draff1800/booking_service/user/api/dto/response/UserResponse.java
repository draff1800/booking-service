package com.draff1800.booking_service.user.api.dto.response;

// Temporary class (Will eventually return a JWT instead)
public record UserResponse(
    String id,
    String email,
    String role
) {}
