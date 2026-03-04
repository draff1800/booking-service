package com.draff1800.booking_service.event.api;

import com.draff1800.booking_service.event.api.dto.request.CreateEventRequest;
import com.draff1800.booking_service.event.api.dto.request.CreateTicketTypeRequest;
import com.draff1800.booking_service.event.api.dto.request.PatchEventRequest;
import com.draff1800.booking_service.event.api.dto.response.TicketTypeResponse;
import com.draff1800.booking_service.event.api.dto.response.eventResponse.EventResponse;
import com.draff1800.booking_service.event.api.dto.response.eventResponse.Organizer;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.domain.TicketType;
import com.draff1800.booking_service.event.service.EventService;
import com.draff1800.booking_service.event.service.EventService.EventWithOrganizer;
import com.draff1800.booking_service.security.jwt.AuthPrincipal;
import com.draff1800.booking_service.user.domain.User;

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
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
    @Valid @RequestBody CreateEventRequest req
  ) {

    Event event = eventService.create(
      req.title(), 
      req.description(), 
      req.venue(), 
      req.startsAt(), 
      req.endsAt(), 
      principal.userId(), 
      idempotencyKey
    );

    return toResponse(eventService.wrapWithOrganizer(event));
  }

  @PostMapping("/{id}/publish")
  public EventResponse publish(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
    Event event = eventService.publish(id, principal.userId());
    return toResponse(eventService.wrapWithOrganizer(event));
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

    return toResponse(eventService.wrapWithOrganizer(updatedEvent));
  }

  @PostMapping("/{id}/cancel")
  public EventResponse cancel(
    @PathVariable UUID id,
    @AuthenticationPrincipal AuthPrincipal principal
  ) {
    Event event = eventService.cancel(id, principal.userId());
    return toResponse(eventService.wrapWithOrganizer(event));
  }

  @PostMapping("/{id}/ticket-types")
  public TicketTypeResponse addTicketType(
    @PathVariable UUID id,
    @AuthenticationPrincipal AuthPrincipal principal,
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
    @Valid @RequestBody CreateTicketTypeRequest req
  ) {
    TicketType ticketType = eventService.addTicketType(
        id,
        principal.userId(),
        req.name(),
        req.priceMinor(),
        req.currency(),
        req.capacityTotal(),
        idempotencyKey
    );
    return toResponse(ticketType);
  }

  @GetMapping("/{id}/ticket-types")
  public List<TicketTypeResponse> listTicketTypes(@PathVariable UUID id) {
    return eventService.listTicketTypes(id).stream().map(this::toResponse).toList();
  }

  private Organizer organizerOrPlaceholder(User organizer) {
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

  private EventResponse toResponse(EventWithOrganizer eventWithOrganizer) {
    Event event = eventWithOrganizer.event();

    return new EventResponse(
      event.getId().toString(),
      event.getTitle(),
      event.getDescription(),
      event.getVenue(),
      event.getStartsAt(),
      event.getEndsAt(),
      event.getStatus(),
      organizerOrPlaceholder(eventWithOrganizer.organizer())
    );
  }

  private TicketTypeResponse toResponse(TicketType ticketType) {
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
