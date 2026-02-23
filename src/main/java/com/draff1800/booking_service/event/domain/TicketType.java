package com.draff1800.booking_service.event.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_types")
public class TicketType {

  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "price_minor", nullable = false)
  private int priceMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "capacity_total", nullable = false)
  private int capacityTotal;

  @Column(name = "capacity_remaining", nullable = false)
  private int capacityRemaining;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TicketType() {}

  public TicketType(UUID eventId, String name, int priceMinor, String currency, int capacityTotal) {
    this.eventId = eventId;
    this.name = name;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.capacityTotal = capacityTotal;
    this.capacityRemaining = capacityTotal;
  }

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    if (id == null) id = UUID.randomUUID();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public UUID getEventId() { return eventId; }
  public String getName() { return name; }
  public int getPriceMinor() { return priceMinor; }
  public String getCurrency() { return currency; }
  public int getCapacityTotal() { return capacityTotal; }
  public int getCapacityRemaining() { return capacityRemaining; }
}
