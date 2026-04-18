package com.draff1800.booking_service.messaging;

public final class RabbitMqMessagingTopology {
  private RabbitMqMessagingTopology() {}

  public static final String BOOKING_DOMAIN_EVENTS_EXCHANGE = "booking.domain.events";
  public static final String BOOKING_EVENTS_DLX = "booking.domain.events.dlx";

  public static final String BOOKING_CONFIRMED_EVENT_TYPE = "booking.confirmed.v1";

  public static final String BOOKING_CONFIRMED_QUEUE = "booking.notification.booking-confirmed.v1";
  public static final String BOOKING_CONFIRMED_ROUTING_KEY = "booking.confirmed";

  public static final String BOOKING_CONFIRMED_DLQ = "booking.notification.booking-confirmed.dlq";
  public static final String BOOKING_CONFIRMED_DLQ_ROUTING_KEY = "booking.confirmed.dlq";
}
