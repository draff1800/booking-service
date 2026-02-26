package com.draff1800.booking_service.booking.api;

import com.draff1800.booking_service.booking.api.dto.request.CreateBookingRequest;
import com.draff1800.booking_service.booking.api.dto.response.BookingItemResponse;
import com.draff1800.booking_service.booking.api.dto.response.BookingResponse;
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

  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
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

    BookingService.BookingResult result = bookingService.createBooking(
      principal.userId(), 
      idempotencyKey, 
      ticketTypeQuantitiesById
    );

    var bookingItems = result.items().stream()
        .map(i -> new BookingItemResponse(
            i.getTicketTypeId().toString(),
            i.getQuantity(),
            i.getUnitPriceMinor(),
            i.getCurrency()
        ))
        .toList();

    return new BookingResponse(
        result.booking().getId().toString(),
        result.booking().getStatus().name(),
        bookingItems
    );
  }

  @GetMapping("/mine")
  public Page<BookingResponse> mine(@AuthenticationPrincipal AuthPrincipal principal, Pageable pageable) {
    return bookingService.listMine(principal.userId(), pageable)
      .map(booking -> new BookingResponse(
        booking.booking().getId().toString(),
        booking.booking().getStatus().name(),
        booking.items().stream().map(bookingItem -> new BookingItemResponse(
          bookingItem.getTicketTypeId().toString(),
          bookingItem.getQuantity(),
          bookingItem.getUnitPriceMinor(),
          bookingItem.getCurrency()
        )).toList()
      ));
  }
}
