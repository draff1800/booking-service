package com.draff1800.booking_service.event.api.dto.response;

public record TicketTypeResponse(
    String id,
    String eventId,
    String name,
    int priceMinor,
    String currency,
    int capacityTotal,
    int capacityRemaining
) {}
