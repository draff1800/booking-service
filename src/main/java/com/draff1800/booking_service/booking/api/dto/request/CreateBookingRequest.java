package com.draff1800.booking_service.booking.api.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

  public record CreateBookingRequest(
      @NotEmpty(message = "items must not be empty")
      @Valid
      List<BookingItemRequest> items
  ) {}
