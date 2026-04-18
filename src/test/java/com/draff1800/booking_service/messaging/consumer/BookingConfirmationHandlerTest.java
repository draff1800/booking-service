package com.draff1800.booking_service.messaging.consumer;

import com.draff1800.booking_service.messaging.RabbitMqMessagingTopology;
import com.draff1800.booking_service.messaging.booking.BookingConfirmedEvent;
import com.draff1800.booking_service.messaging.consumer.repo.BookingConfirmedDispatchRepository;
import com.draff1800.booking_service.messaging.consumer.repo.ProcessedMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingConfirmationHandlerTest {

  @Mock private BookingConfirmedDispatchRepository dispatchRepository;
  @Mock private ProcessedMessageRepository processedMessageRepository;

  @InjectMocks
  private BookingConfirmationHandler handler;

  @Test
  void handle_skipsAlreadyProcessedMessages() {
    BookingConfirmedEvent event = exampleEvent();
    when(processedMessageRepository.existsById(event.eventId())).thenReturn(true);

    handler.handle(event);

    verify(dispatchRepository, never()).save(any());
    verify(processedMessageRepository, never()).save(any());
  }

  @Test
  void handle_createsDispatchAndProcessedMarker() {
    BookingConfirmedEvent event = exampleEvent();
    when(processedMessageRepository.existsById(event.eventId())).thenReturn(false);
    when(dispatchRepository.existsByBookingId(event.bookingId())).thenReturn(false);

    handler.handle(event);

    verify(dispatchRepository).save(any());
    verify(processedMessageRepository).save(any());
  }

  @Test
  void handle_avoidsDuplicateDispatchWhenDispatchExists() {
    BookingConfirmedEvent event = exampleEvent();
    when(processedMessageRepository.existsById(event.eventId())).thenReturn(false);
    when(dispatchRepository.existsByBookingId(event.bookingId())).thenReturn(true);

    handler.handle(event);

    verify(dispatchRepository, never()).save(any());
    verify(processedMessageRepository).save(any());
  }

  private BookingConfirmedEvent exampleEvent() {
    return new BookingConfirmedEvent(
      UUID.randomUUID(),
      RabbitMqMessagingTopology.BOOKING_CONFIRMED_EVENT_TYPE,
      Instant.now(),
      UUID.randomUUID(),
      UUID.randomUUID(),
      "CONFIRMED",
      "booking-key-2",
      List.of()
    );
  }
}
