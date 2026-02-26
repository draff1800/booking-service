package com.draff1800.booking_service.booking.repo;

import com.draff1800.booking_service.booking.domain.Booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Booking> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
