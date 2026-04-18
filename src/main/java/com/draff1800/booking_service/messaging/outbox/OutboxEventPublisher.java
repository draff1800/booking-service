package com.draff1800.booking_service.messaging.outbox;

import com.draff1800.booking_service.messaging.booking.BookingConfirmedEvent;
import com.draff1800.booking_service.messaging.outbox.domain.OutboxEvent;
import com.draff1800.booking_service.messaging.outbox.repo.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.messaging.rabbit.enabled", havingValue = "true")
public class OutboxEventPublisher {

  private final OutboxEventRepository outboxEventRepository;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public OutboxEventPublisher(
    OutboxEventRepository outboxEventRepository,
    RabbitTemplate rabbitTemplate,
    ObjectMapper objectMapper
  ) {
    this.outboxEventRepository = outboxEventRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${app.messaging.outbox.publish-delay-ms:5000}")
  public void publishPendingEvents() {
    List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByOccurredAtAscIdAsc();
    for (OutboxEvent pendingEvent : pendingEvents) {
      publishSingleEvent(pendingEvent);
    }
  }

  @Transactional
  public void publishSingleEvent(OutboxEvent outboxEvent) {
    try {
      BookingConfirmedEvent event = objectMapper.readValue(outboxEvent.getPayload(), BookingConfirmedEvent.class);
      rabbitTemplate.convertAndSend(
        outboxEvent.getExchangeName(),
        outboxEvent.getRoutingKey(),
        event,
        message -> messageWithMessageId(message, outboxEvent.getId().toString())
      );
      outboxEvent.markPublished(Instant.now());
    } catch (Exception exception) {
      outboxEvent.markPublishFailure(exception.getMessage());
    }

    outboxEventRepository.save(outboxEvent);
  }

  private Message messageWithMessageId(Message message, String messageId) {
    message.getMessageProperties().setMessageId(messageId);
    return message;
  }
}
