package com.draff1800.booking_service.event.api;

import com.draff1800.booking_service.event.api.dto.request.CreateEventRequest;
import com.draff1800.booking_service.event.api.dto.request.CreateTicketTypeRequest;
import com.draff1800.booking_service.event.api.dto.request.PatchEventRequest;
import com.draff1800.booking_service.event.api.dto.response.EventResponse;
import com.draff1800.booking_service.event.api.dto.response.TicketTypeResponse;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.domain.TicketType;
import com.draff1800.booking_service.event.service.EventService;
import com.draff1800.booking_service.security.jwt.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

  @PostMapping("/{id}/publish")
  public EventResponse publish(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
    return toResponse(eventService.publish(id, principal.userId()));
  }

  @GetMapping("/{id}")
  public EventResponse get(@PathVariable UUID id) {
    return toResponse(eventService.get(id));
  }

  @GetMapping
  public Page<EventResponse> listPublicUpcoming(Pageable pageable) {
    return eventService.listPublicUpcoming(pageable).map(this::toResponse);
  }

  @GetMapping("/mine")
  public Page<EventResponse> listMine(@AuthenticationPrincipal AuthPrincipal principal, Pageable pageable) {
    return eventService.listMine(principal.userId(), pageable).map(this::toResponse);
  }

  @PatchMapping("/{id}")
  public EventResponse updateDetails(
    @PathVariable UUID id,
    @AuthenticationPrincipal AuthPrincipal principal,
    @Valid @RequestBody PatchEventRequest req
  ) {

    Event updatedEvent = eventService.updateDetails(
        id,
        principal.userId(),
        req.title(),
        req.description(),
        req.venue(),
        req.startsAt(),
        req.endsAt()
    );

    return toResponse(updatedEvent);
  }

  @PostMapping("/{id}/cancel")
  public EventResponse cancel(
    @PathVariable UUID id,
    @AuthenticationPrincipal AuthPrincipal principal
  ) {
    return toResponse(eventService.cancel(id, principal.userId()));
  }

  @PostMapping("/{id}/ticket-types")
  public TicketTypeResponse addTicketType(
    @PathVariable UUID id,
    @AuthenticationPrincipal AuthPrincipal principal,
    @Valid @RequestBody CreateTicketTypeRequest req
  ) {
    TicketType ticketType = eventService.addTicketType(
        id,
        principal.userId(),
        req.name(),
        req.priceMinor(),
        req.currency(),
        req.capacityTotal()
    );
    return toResponse(ticketType);
  }

  @GetMapping("/{id}/ticket-types")
  public List<TicketTypeResponse> listTicketTypes(@PathVariable UUID id) {
    return eventService.listTicketTypes(id).stream().map(this::toResponse).toList();
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

  private TicketTypeResponse toResponse(TicketType tt) {
    return new TicketTypeResponse(
      tt.getId().toString(),
      tt.getEventId().toString(),
      tt.getName(),
      tt.getPriceMinor(),
      tt.getCurrency(),
      tt.getCapacityTotal(),
      tt.getCapacityRemaining()
    );
  }
}
