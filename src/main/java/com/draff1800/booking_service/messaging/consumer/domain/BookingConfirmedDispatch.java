package com.draff1800.booking_service.messaging.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_confirmed_dispatches")
public class BookingConfirmedDispatch {

  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(name = "booking_id", nullable = false, unique = true)
  private UUID bookingId;

  @Column(name = "source_event_id", nullable = false, unique = true)
  private UUID sourceEventId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  protected BookingConfirmedDispatch() {}

  public BookingConfirmedDispatch(UUID bookingId, UUID sourceEventId, UUID userId) {
    this.bookingId = bookingId;
    this.sourceEventId = sourceEventId;
    this.userId = userId;
    this.status = "READY";
  }

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (processedAt == null) {
      processedAt = Instant.now();
    }
  }
}
