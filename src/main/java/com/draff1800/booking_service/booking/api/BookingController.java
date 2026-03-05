package com.draff1800.booking_service.booking.api;

import com.draff1800.booking_service.booking.api.dto.request.CreateBookingRequest;
import com.draff1800.booking_service.booking.api.dto.response.BookingResponse;
import com.draff1800.booking_service.booking.api.mapper.BookingResponseMapper;
import com.draff1800.booking_service.booking.service.BookingService;
import com.draff1800.booking_service.security.jwt.AuthPrincipal;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {

  private final BookingService bookingService;
  private final BookingResponseMapper mapper;

  public BookingController(BookingService bookingService, BookingResponseMapper mapper) {
    this.bookingService = bookingService;
    this.mapper = mapper;
  }

  @PostMapping
  public BookingResponse create(
    @AuthenticationPrincipal AuthPrincipal principal,
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
    @Valid @RequestBody CreateBookingRequest req
  ) {

    Map<UUID, Integer> ticketTypeQuantitiesById = new LinkedHashMap<>();
    for (var item : req.items()) {
      UUID ticketTypeId = UUID.fromString(item.ticketTypeId());
      int quantity = item.quantity();
      ticketTypeQuantitiesById.merge(ticketTypeId, quantity, Integer::sum);
    }

    BookingService.BookingWithItems bookingWithItems = bookingService.createBooking(
      principal.userId(), 
      idempotencyKey, 
      ticketTypeQuantitiesById
    );

    return mapper.toResponse(bookingWithItems);
  }

  @GetMapping("/mine")
  public Page<BookingResponse> mine(@AuthenticationPrincipal AuthPrincipal principal, Pageable pageable) {
    return bookingService.listMine(principal.userId(), pageable).map(
      booking -> mapper.toResponse(booking)
    );
  }
}
