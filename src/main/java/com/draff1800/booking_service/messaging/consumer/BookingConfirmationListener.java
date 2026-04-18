package com.draff1800.booking_service.messaging.consumer;

import com.draff1800.booking_service.messaging.RabbitMqMessagingTopology;
import com.draff1800.booking_service.messaging.booking.BookingConfirmedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.rabbit.enabled", havingValue = "true")
public class BookingConfirmationListener {

  private final BookingConfirmationHandler bookingConfirmationHandler;

  public BookingConfirmationListener(BookingConfirmationHandler bookingConfirmationHandler) {
    this.bookingConfirmationHandler = bookingConfirmationHandler;
  }

  @RabbitListener(queues = RabbitMqMessagingTopology.BOOKING_CONFIRMED_QUEUE)
  public void onBookingConfirmed(BookingConfirmedEvent event) {
    bookingConfirmationHandler.handle(event);
  }
}
