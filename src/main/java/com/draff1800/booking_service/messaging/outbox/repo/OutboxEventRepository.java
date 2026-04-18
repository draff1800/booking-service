package com.draff1800.booking_service.messaging.outbox.repo;

import com.draff1800.booking_service.messaging.outbox.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
  List<OutboxEvent> findTop50ByPublishedAtIsNullOrderByOccurredAtAscIdAsc();
}
