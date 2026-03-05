package com.draff1800.booking_service.event.api.mapper;

import org.springframework.stereotype.Component;

import com.draff1800.booking_service.event.api.dto.response.eventResponse.EventResponse;
import com.draff1800.booking_service.event.api.dto.response.eventResponse.Organizer;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.service.EventService.EventWithOrganizer;
import com.draff1800.booking_service.user.domain.User;

@Component
public class EventResponseMapper {
    
    public EventResponse toResponse(EventWithOrganizer eventWithOrganizer) {
        Event event = eventWithOrganizer.event();

        return new EventResponse(
            event.getId().toString(),
            event.getTitle(),
            event.getDescription(),
            event.getVenue(),
            event.getStartsAt(),
            event.getEndsAt(),
            event.getStatus(),
            getOrganizerDetails(eventWithOrganizer.organizer())
        );
    }

    private Organizer getOrganizerDetails(User organizer) {
        if (organizer == null) {
        return new Organizer(null, "Deleted User");
        }
        
        String handle = (organizer.getHandle() == null || organizer.getHandle().isBlank())
            ? null
            : organizer.getHandle();
        String displayName = (organizer.getDisplayName() == null || organizer.getDisplayName().isBlank())
            ? "Deleted User"
            : organizer.getDisplayName();

        return new Organizer(displayName, handle);
    }
}
