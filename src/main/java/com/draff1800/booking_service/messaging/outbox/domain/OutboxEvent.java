package com.draff1800.booking_service.messaging.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(name = "aggregate_type", nullable = false, length = 100)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "exchange_name", nullable = false, length = 200)
  private String exchangeName;

  @Column(name = "routing_key", nullable = false, length = 200)
  private String routingKey;

  @Column(nullable = false, columnDefinition = "text")
  private String payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "publish_attempts", nullable = false)
  private int publishAttempts;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  protected OutboxEvent() {}

  public OutboxEvent(
    UUID id,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    String exchangeName,
    String routingKey,
    String payload,
    Instant occurredAt
  ) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.exchangeName = exchangeName;
    this.routingKey = routingKey;
    this.payload = payload;
    this.occurredAt = occurredAt;
  }

  @PrePersist
  void prePersist() {
    if (publishAttempts < 0) {
      publishAttempts = 0;
    }
  }

  public UUID getId() { return id; }
  public String getExchangeName() { return exchangeName; }
  public String getRoutingKey() { return routingKey; }
  public String getPayload() { return payload; }
  public Instant getOccurredAt() { return occurredAt; }

  public void markPublished(Instant publishedAt) {
    this.publishedAt = publishedAt;
    this.lastError = null;
  }

  public void markPublishFailure(String errorMessage) {
    this.publishAttempts += 1;
    this.lastError = errorMessage;
  }
}
