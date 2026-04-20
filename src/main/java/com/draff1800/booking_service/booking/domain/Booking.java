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

  @Column(name = "idempotency_key", length = 100)
  private String idempotencyKey;

  @Column(name = "request_fingerprint", length = 64)
  private String requestFingerprint;

  protected Booking() {}

  public Booking(UUID userId, String idempotencyKey, String requestFingerprint) {
    this.userId = userId;
    this.status = BookingStatus.CONFIRMED;
    this.idempotencyKey = idempotencyKey;
    this.requestFingerprint = requestFingerprint;
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
  public Instant getCreatedAt() { return createdAt; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public String getRequestFingerprint() { return requestFingerprint; }
}
