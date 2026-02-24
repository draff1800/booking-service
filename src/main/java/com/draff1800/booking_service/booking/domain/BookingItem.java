package com.draff1800.booking_service.booking.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_items")
public class BookingItem {

  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(name = "booking_id", nullable = false)
  private UUID bookingId;

  @Column(name = "ticket_type_id", nullable = false)
  private UUID ticketTypeId;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price_minor", nullable = false)
  private int unitPriceMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected BookingItem() {}

  public BookingItem(UUID bookingId, UUID ticketTypeId, int quantity, int unitPriceMinor, String currency) {
    this.bookingId = bookingId;
    this.ticketTypeId = ticketTypeId;
    this.quantity = quantity;
    this.unitPriceMinor = unitPriceMinor;
    this.currency = currency;
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
  public UUID getBookingId() { return bookingId; }
  public UUID getTicketTypeId() { return ticketTypeId; }
  public int getQuantity() { return quantity; }
  public int getUnitPriceMinor() { return unitPriceMinor; }
  public String getCurrency() { return currency; }
}