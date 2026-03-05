package com.draff1800.booking_service.booking.api.mapper;

import org.springframework.stereotype.Component;

import com.draff1800.booking_service.booking.api.dto.response.BookingItemResponse;
import com.draff1800.booking_service.booking.api.dto.response.BookingResponse;
import com.draff1800.booking_service.booking.service.BookingService;

@Component
public class BookingResponseMapper {

    public BookingResponse toResponse(BookingService.BookingWithItems bookingWithItems) {
        var bookingItems = bookingWithItems.items().stream()
            .map(i -> new BookingItemResponse(
                i.getTicketTypeId().toString(),
                i.getQuantity(),
                i.getUnitPriceMinor(),
                i.getCurrency()
            ))
            .toList();

        return new BookingResponse(
            bookingWithItems.booking().getId().toString(),
            bookingWithItems.booking().getStatus().name(),
            bookingItems
        );
    }
}
