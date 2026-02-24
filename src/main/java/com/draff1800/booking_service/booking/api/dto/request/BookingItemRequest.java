package com.draff1800.booking_service.booking.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingItemRequest(
    @NotBlank(message = "ticketTypeId is required")
    String ticketTypeId,

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be >= 1")
    Integer quantity
) {}
