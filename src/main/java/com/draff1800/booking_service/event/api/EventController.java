package com.draff1800.booking_service.event.api;

import com.draff1800.booking_service.event.api.dto.request.CreateEventRequest;
import com.draff1800.booking_service.event.api.dto.response.EventResponse;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.service.EventService;
import com.draff1800.booking_service.security.jwt.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  @PostMapping
  public EventResponse create(
    @AuthenticationPrincipal AuthPrincipal principal,
    @Valid @RequestBody CreateEventRequest req
  ) {
    Event e = eventService.create(req.title(), req.description(), req.venue(), req.startsAt(), req.endsAt(), principal.userId());
    return toResponse(e);
  }

  @GetMapping("/{id}")
  public EventResponse get(@PathVariable UUID id) {
    return toResponse(eventService.get(id));
  }

  @GetMapping
  public Page<EventResponse> list(Pageable pageable) {
    return eventService.listPublicUpcoming(pageable).map(this::toResponse);
  }

  @PostMapping("/{id}/publish")
  public EventResponse publish(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
    return toResponse(eventService.publish(id, principal.userId()));
  }

  private EventResponse toResponse(Event event) {
    return new EventResponse(
        event.getId().toString(),
        event.getTitle(),
        event.getDescription(),
        event.getVenue(),
        event.getStartsAt(),
        event.getEndsAt(),
        event.getStatus()
    );
  }
}
