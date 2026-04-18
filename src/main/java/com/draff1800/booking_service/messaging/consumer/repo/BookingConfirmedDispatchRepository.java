package com.draff1800.booking_service.messaging.consumer.repo;

import com.draff1800.booking_service.messaging.consumer.domain.BookingConfirmedDispatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingConfirmedDispatchRepository extends JpaRepository<BookingConfirmedDispatch, UUID> {
  boolean existsByBookingId(UUID bookingId);
}
