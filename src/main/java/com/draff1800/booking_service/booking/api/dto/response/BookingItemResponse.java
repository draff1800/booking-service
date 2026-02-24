package com.draff1800.booking_service.booking.api.dto.response;

public record BookingItemResponse(
    String ticketTypeId,
    int quantity,
    int unitPriceMinor,
    String currency
) {}

