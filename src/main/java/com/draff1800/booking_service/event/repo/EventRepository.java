package com.draff1800.booking_service.event.repo;

import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
  Page<Event> findByStatusAndStartsAtAfterOrderByStartsAtAsc(EventStatus status, Instant now, Pageable pageable);
}
