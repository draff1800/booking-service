package com.draff1800.booking_service.event.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(length = 200)
  private String venue;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EventStatus status;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Event() {}

  public Event(String title, String description, String venue, Instant startsAt, Instant endsAt, UUID createdBy) {
    this.title = title;
    this.description = description;
    this.venue = venue;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.createdBy = createdBy;
    this.status = EventStatus.DRAFT;
  }

  @PrePersist
  void prePersist() {
    Instant currentInstant = Instant.now();
    if (id == null) id = UUID.randomUUID();
    if (status == null) status = EventStatus.DRAFT;
    createdAt = currentInstant;
    updatedAt = currentInstant;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public String getVenue() { return venue; }
  public Instant getStartsAt() { return startsAt; }
  public Instant getEndsAt() { return endsAt; }
  public EventStatus getStatus() { return status; }
  public UUID getCreatedBy() { return createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void publish() {
    this.status = EventStatus.PUBLISHED;
  }

  public void updateDetails(String title, String description, String venue, Instant startsAt, Instant endsAt) {
    this.title = title;
    this.description = description;
    this.venue = venue;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
  }

  public void cancel() {
    this.status = EventStatus.CANCELLED;
  }
}
