package com.draff1800.booking_service.event.api.dto.response;

import com.draff1800.booking_service.event.domain.EventStatus;

import java.time.Instant;

public record EventResponse(
    String id,
    String title,
    String description,
    String venue,
    Instant startsAt,
    Instant endsAt,
    EventStatus status
) {}
