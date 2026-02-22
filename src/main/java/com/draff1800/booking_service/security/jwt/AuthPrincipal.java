package com.draff1800.booking_service.security.jwt;

import java.util.UUID;

public record AuthPrincipal(
    UUID userId,
    String email,
    String role
 ) {}
