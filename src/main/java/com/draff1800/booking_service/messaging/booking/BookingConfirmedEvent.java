package com.draff1800.booking_service.messaging.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingConfirmedEvent(
  UUID eventId,
  String eventType,
  Instant occurredAt,
  UUID bookingId,
  UUID userId,
  String status,
  String idempotencyKey,
  List<Item> items
) {
  public record Item(
    UUID ticketTypeId,
    int quantity,
    int unitPriceMinor,
    String currency
  ) {}
}
