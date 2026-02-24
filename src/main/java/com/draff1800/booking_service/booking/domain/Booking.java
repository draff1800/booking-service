package com.draff1800.booking_service.booking.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BookingStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Booking() {}

  public Booking(UUID userId) {
    this.userId = userId;
    this.status = BookingStatus.CONFIRMED;
  }

  @PrePersist
  void prePersist() {
    Instant currentInstant = Instant.now();
    if (id == null) id = UUID.randomUUID();
    createdAt = currentInstant;
    updatedAt = currentInstant;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public BookingStatus getStatus() { return status; }
}
