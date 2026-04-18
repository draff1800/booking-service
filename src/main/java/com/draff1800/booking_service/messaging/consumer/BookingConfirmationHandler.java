package com.draff1800.booking_service.messaging.consumer;

import com.draff1800.booking_service.messaging.booking.BookingConfirmedEvent;
import com.draff1800.booking_service.messaging.consumer.domain.BookingConfirmedDispatch;
import com.draff1800.booking_service.messaging.consumer.domain.ProcessedMessage;
import com.draff1800.booking_service.messaging.consumer.repo.BookingConfirmedDispatchRepository;
import com.draff1800.booking_service.messaging.consumer.repo.ProcessedMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingConfirmationHandler {

  private final BookingConfirmedDispatchRepository dispatchRepository;
  private final ProcessedMessageRepository processedMessageRepository;

  public BookingConfirmationHandler(
    BookingConfirmedDispatchRepository dispatchRepository,
    ProcessedMessageRepository processedMessageRepository
  ) {
    this.dispatchRepository = dispatchRepository;
    this.processedMessageRepository = processedMessageRepository;
  }

  @Transactional
  public void handle(BookingConfirmedEvent event) {
    if (processedMessageRepository.existsById(event.eventId())) {
      return;
    }

    if (!dispatchRepository.existsByBookingId(event.bookingId())) {
      dispatchRepository.save(new BookingConfirmedDispatch(
        event.bookingId(),
        event.eventId(),
        event.userId()
      ));
    }

    processedMessageRepository.save(new ProcessedMessage(event.eventId()));
  }
}
