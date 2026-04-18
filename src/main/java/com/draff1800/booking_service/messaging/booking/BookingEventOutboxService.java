package com.draff1800.booking_service.messaging.booking;

import com.draff1800.booking_service.booking.domain.Booking;
import com.draff1800.booking_service.booking.domain.BookingItem;
import com.draff1800.booking_service.messaging.RabbitMqMessagingTopology;
import com.draff1800.booking_service.messaging.outbox.domain.OutboxEvent;
import com.draff1800.booking_service.messaging.outbox.repo.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookingEventOutboxService {

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public BookingEventOutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  public void enqueueBookingConfirmedEvent(Booking booking, List<BookingItem> bookingItems) {
    UUID eventId = UUID.randomUUID();
    BookingConfirmedEvent event = new BookingConfirmedEvent(
      eventId,
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_EVENT_TYPE,
      booking.getCreatedAt(),
      booking.getId(),
      booking.getUserId(),
      booking.getStatus().name(),
      booking.getIdempotencyKey(),
      bookingItems.stream()
        .map(item -> new BookingConfirmedEvent.Item(
          item.getTicketTypeId(),
          item.getQuantity(),
          item.getUnitPriceMinor(),
          item.getCurrency()
        ))
        .toList()
    );

    outboxEventRepository.save(new OutboxEvent(
      eventId,
      "booking",
      booking.getId(),
      event.eventType(),
      RabbitMqMessagingTopology.BOOKING_DOMAIN_EVENTS_EXCHANGE,
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_ROUTING_KEY,
      serialize(event),
      event.occurredAt()
    ));
  }

  private String serialize(BookingConfirmedEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize booking outbox event", exception);
    }
  }
}
