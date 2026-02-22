package com.draff1800.booking_service.event.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

  public record CreateEventRequest(
      @NotBlank(message = "title is required")
      @Size(max = 200, message = "title must be less that 200 characters")
      String title,

      String description,
      String venue,

      @NotNull(message = "startsAt is required")
      Instant startsAt,

      @NotNull(message = "endsAt is required")
      Instant endsAt
  ) {}
