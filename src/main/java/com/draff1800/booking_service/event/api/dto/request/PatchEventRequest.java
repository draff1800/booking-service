package com.draff1800.booking_service.event.api.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.Size;

public record PatchEventRequest(
  @Size(max = 200, message = "title must be less than 200 characters")
  String title,
  String description,
  String venue,
  Instant startsAt,
  Instant endsAt
) {}
