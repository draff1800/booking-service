package com.draff1800.booking_service.event.api;

import com.draff1800.booking_service.event.api.dto.request.CreateEventRequest;
import com.draff1800.booking_service.event.api.dto.request.CreateTicketTypeRequest;
import com.draff1800.booking_service.event.api.dto.request.PatchEventRequest;
import com.draff1800.booking_service.event.api.dto.response.TicketTypeResponse;
import com.draff1800.booking_service.event.api.dto.response.eventResponse.EventResponse;
import com.draff1800.booking_service.event.api.mapper.EventResponseMapper;
import com.draff1800.booking_service.event.api.mapper.TicketTypeResponseMapper;
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
public class EventsController {

  private final EventService eventService;
  private final EventResponseMapper eventResponseMapper;
  private final TicketTypeResponseMapper ticketTypeResponseMapper;

  public EventsController(
    EventService eventService,
    EventResponseMapper eventResponseMapper,
    TicketTypeResponseMapper ticketTypeResponseMapper
  ) {
    this.eventService = eventService;
    this.eventResponseMapper = eventResponseMapper;
    this.ticketTypeResponseMapper = ticketTypeResponseMapper;
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

    return eventResponseMapper.toResponse(
      eventService.wrapWithOrganizer(event)
    );
  }

  @PostMapping("/{id}/publish")
  public EventResponse publish(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
    Event event = eventService.publish(id, principal.userId());
    return eventResponseMapper.toResponse(
      eventService.wrapWithOrganizer(event)
    );
  }

  @GetMapping("/{id}")
  public EventResponse get(@PathVariable UUID id) {
    return eventResponseMapper.toResponse(
      eventService.get(id)
    );
  }

  @GetMapping
  public Page<EventResponse> listPublicUpcoming(Pageable pageable) {
    return eventService.listPublicUpcoming(pageable).map(eventResponseMapper::toResponse);
  }

  @GetMapping("/mine")
  public Page<EventResponse> listMine(@AuthenticationPrincipal AuthPrincipal principal, Pageable pageable) {
    return eventService.listMine(principal.userId(), pageable).map(eventResponseMapper::toResponse);
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

    return eventResponseMapper.toResponse(
      eventService.wrapWithOrganizer(updatedEvent)
    );
  }

  @PostMapping("/{id}/cancel")
  public EventResponse cancel(
    @PathVariable UUID id,
    @AuthenticationPrincipal AuthPrincipal principal
  ) {
    Event event = eventService.cancel(id, principal.userId());

    return eventResponseMapper.toResponse(
      eventService.wrapWithOrganizer(event)
    );
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
    return ticketTypeResponseMapper.toResponse(ticketType);
  }

  @GetMapping("/{id}/ticket-types")
  public List<TicketTypeResponse> listTicketTypes(@PathVariable UUID id) {
    return eventService.listTicketTypes(id).stream().map(ticketTypeResponseMapper::toResponse).toList();
  }
}
