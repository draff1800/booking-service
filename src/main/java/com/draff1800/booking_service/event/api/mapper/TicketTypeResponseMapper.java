package com.draff1800.booking_service.event.api.mapper;

import org.springframework.stereotype.Component;

import com.draff1800.booking_service.event.api.dto.response.TicketTypeResponse;
import com.draff1800.booking_service.event.domain.TicketType;

@Component
public class TicketTypeResponseMapper {

    public TicketTypeResponse toResponse(TicketType ticketType) {
        return new TicketTypeResponse(
            ticketType.getId().toString(),
            ticketType.getEventId().toString(),
            ticketType.getName(),
            ticketType.getPriceMinor(),
            ticketType.getCurrency(),
            ticketType.getCapacityTotal(),
            ticketType.getCapacityRemaining()
        );
    }
}
