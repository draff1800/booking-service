package com.draff1800.booking_service.messaging.outbox;

import com.draff1800.booking_service.messaging.RabbitMqMessagingTopology;
import com.draff1800.booking_service.messaging.booking.BookingConfirmedEvent;
import com.draff1800.booking_service.messaging.outbox.domain.OutboxEvent;
import com.draff1800.booking_service.messaging.outbox.repo.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

  @Mock private OutboxEventRepository outboxEventRepository;
  @Mock private RabbitTemplate rabbitTemplate;
  private OutboxEventPublisher outboxPublisher;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void publishSingleEvent_marksEventPublishedAndSetsMessageId() throws Exception {
    BookingConfirmedEvent event = sampleEvent();
    OutboxEvent outboxEvent = new OutboxEvent(
      event.eventId(),
      "booking",
      event.bookingId(),
      event.eventType(),
      RabbitMqMessagingTopology.BOOKING_DOMAIN_EVENTS_EXCHANGE,
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_ROUTING_KEY,
      objectMapper.writeValueAsString(event),
      event.occurredAt()
    );

    outboxPublisher = new OutboxEventPublisher(outboxEventRepository, rabbitTemplate, objectMapper);

    ArgumentCaptor<MessagePostProcessor> postProcessorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

    outboxPublisher.publishSingleEvent(outboxEvent);

    verify(rabbitTemplate).convertAndSend(
      anyString(),
      anyString(),
      any(BookingConfirmedEvent.class),
      postProcessorCaptor.capture()
    );
    verify(outboxEventRepository).save(outboxEvent);

    Message processedMessage = postProcessorCaptor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
    assertThat(processedMessage.getMessageProperties().getMessageId()).isEqualTo(event.eventId().toString());
  }

  @Test
  void publishSingleEvent_recordsFailureWhenPublishFails() throws Exception {
    BookingConfirmedEvent event = sampleEvent();
    OutboxEvent outboxEvent = new OutboxEvent(
      event.eventId(),
      "booking",
      event.bookingId(),
      event.eventType(),
      RabbitMqMessagingTopology.BOOKING_DOMAIN_EVENTS_EXCHANGE,
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_ROUTING_KEY,
      objectMapper.writeValueAsString(event),
      event.occurredAt()
    );

    outboxPublisher = new OutboxEventPublisher(outboxEventRepository, rabbitTemplate, objectMapper);
    doThrow(new RuntimeException("broker unavailable")).when(rabbitTemplate).convertAndSend(
      anyString(),
      anyString(),
      any(BookingConfirmedEvent.class),
      any(MessagePostProcessor.class)
    );

    outboxPublisher.publishSingleEvent(outboxEvent);

    verify(outboxEventRepository).save(outboxEvent);
  }

  @Test
  void publishPendingEvents_processesPendingRows() {
    OutboxEvent event1 = new OutboxEvent(
      UUID.randomUUID(),
      "booking1",
      UUID.randomUUID(),
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_EVENT_TYPE,
      RabbitMqMessagingTopology.BOOKING_DOMAIN_EVENTS_EXCHANGE,
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_ROUTING_KEY,
      "{\"broken\":true}",
      Instant.now()
    );
    OutboxEvent event2 = new OutboxEvent(
      UUID.randomUUID(),
      "booking2",
      UUID.randomUUID(),
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_EVENT_TYPE,
      RabbitMqMessagingTopology.BOOKING_DOMAIN_EVENTS_EXCHANGE,
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_ROUTING_KEY,
      "{\"broken\":true}",
      Instant.now()
    );

    outboxPublisher = new OutboxEventPublisher(outboxEventRepository, rabbitTemplate, objectMapper);
    when(outboxEventRepository.findTop50ByPublishedAtIsNullOrderByOccurredAtAscIdAsc()).thenReturn(List.of(event1, event2));

    outboxPublisher.publishPendingEvents();

    verify(outboxEventRepository).save(event1);
    verify(outboxEventRepository).save(event2);
  }

  private BookingConfirmedEvent sampleEvent() {
    return new BookingConfirmedEvent(
      UUID.randomUUID(),
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_EVENT_TYPE,
      Instant.now(),
      UUID.randomUUID(),
      UUID.randomUUID(),
      "CONFIRMED",
      "booking-key-1",
      List.of(new BookingConfirmedEvent.Item(UUID.randomUUID(), 2, 2500, "GBP"))
    );
  }
}
