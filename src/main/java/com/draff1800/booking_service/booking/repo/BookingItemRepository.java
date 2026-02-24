package com.draff1800.booking_service.booking.repo;

import com.draff1800.booking_service.booking.domain.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {
  List<BookingItem> findByBookingId(UUID bookingId);
}
