package com.draff1800.booking_service.messaging.consumer.repo;

import com.draff1800.booking_service.messaging.consumer.domain.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, UUID> {
}
