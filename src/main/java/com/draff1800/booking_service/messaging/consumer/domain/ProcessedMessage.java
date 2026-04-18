package com.draff1800.booking_service.messaging.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {

  @Id
  @Column(nullable = false)
  private UUID messageId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  protected ProcessedMessage() {}

  public ProcessedMessage(UUID messageId) {
    this.messageId = messageId;
  }

  @PrePersist
  void prePersist() {
    if (processedAt == null) {
      processedAt = Instant.now();
    }
  }
}
