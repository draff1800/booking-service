package com.draff1800.booking_service.booking.api.dto.response;

import java.util.List;

public record BookingResponse(
    String id,
    String status,
    List<BookingItemResponse> item
) {}
