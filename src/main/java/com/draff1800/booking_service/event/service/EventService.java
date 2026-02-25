package com.draff1800.booking_service.event.service;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.ForbiddenException;
import com.draff1800.booking_service.common.error.exception.NotFoundException;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.domain.EventStatus;
import com.draff1800.booking_service.event.domain.TicketType;
import com.draff1800.booking_service.event.repo.EventRepository;
import com.draff1800.booking_service.event.repo.TicketTypeRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

  private final EventRepository eventRepository;
  private final TicketTypeRepository ticketTypeRepository;

  public EventService(EventRepository eventRepository, TicketTypeRepository ticketTypeRepository) {
    this.eventRepository = eventRepository;
    this.ticketTypeRepository = ticketTypeRepository;
  }

  @Transactional
  public Event create(String title, String description, String venue, Instant startsAt, Instant endsAt, UUID createdBy) {
    assertValidEventTimes(startsAt, endsAt);

    Event event = new Event(title, description, venue, startsAt, endsAt, createdBy);
    return eventRepository.save(event);
  }

  @Transactional(readOnly = true)
  public Event get(UUID id) {
    return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
  }

  @Transactional(readOnly = true)
  public Page<Event> listMine(UUID userId, Pageable pageable) {
    return eventRepository.findByCreatedByAndStatusInOrderByStartsAtAsc(
        userId,
        List.of(EventStatus.DRAFT, EventStatus.PUBLISHED),
        pageable
    );
  }

  @Transactional(readOnly = true)
  public Page<Event> listPublicUpcoming(Pageable pageable) {
    return eventRepository.findByStatusAndStartsAtAfterOrderByStartsAtAsc(EventStatus.PUBLISHED, Instant.now(), pageable);
  }

  @Transactional
  public Event publish(UUID eventId, UUID requesterUserId) {
    Event event = get(eventId);

    assertUserIsEventCreator(event, requesterUserId);

    if (event.getStatus() == EventStatus.CANCELLED) {
      throw new ConflictException("Cancelled events cannot be published");
    }

    event.publish();
    return eventRepository.save(event);
  }

  @Transactional
  public Event updateDetails(
    UUID eventId, 
    UUID requesterUserId,
    String title, 
    String description, 
    String venue,
    Instant startsAt, 
    Instant endsAt
  ) {

    Event event = get(eventId);

    assertUserIsEventCreator(event, requesterUserId);
    assertEventIsDraft(event, "Updating event");

    String newTitle = (title != null) ? title.trim() : event.getTitle();
    String newDescription = (description != null) ? description : event.getDescription();
    String newVenue = (venue != null) ? venue : event.getVenue();
    Instant newStartsAt = (startsAt != null) ? startsAt : event.getStartsAt();
    Instant newEndsAt = (endsAt != null) ? endsAt : event.getEndsAt();

    assertValidEventTimes(newStartsAt, newEndsAt);

    event.updateDetails(newTitle, newDescription, newVenue, newStartsAt, newEndsAt);
    return eventRepository.save(event);
  }

  @Transactional
  public Event cancel(UUID eventId, UUID requesterUserId) {
    Event event = get(eventId);

    assertUserIsEventCreator(event, requesterUserId);

    if (event.getStatus() == EventStatus.CANCELLED) {
      return event;
    }

    event.cancel();
    return eventRepository.save(event);
  }

  private void assertUserIsEventCreator(Event event, UUID requesterUserId) {
    if (!event.getCreatedBy().equals(requesterUserId)) {
      throw new ForbiddenException("Only the event creator can perform this action");
    }
  }

  private void assertEventIsDraft(Event event, String action) {
    if (event.getStatus() != EventStatus.DRAFT) {
      throw new ConflictException(action + " is only allowed while event is DRAFT");
    }
  }

  private void assertValidEventTimes(Instant startsAt, Instant endsAt) {
    if (!endsAt.isAfter(startsAt)) {
      throw new ConflictException("endsAt must be after startsAt");
    }
  }

  @Transactional
  public TicketType addTicketType(UUID eventId, UUID requesterUserId, String name, int priceMinor, String currency, int capacityTotal) {
    Event event = get(eventId);

    if (!event.getCreatedBy().equals(requesterUserId)) {
      throw new ForbiddenException("Only the event creator can add ticket types");
    }

    if (event.getStatus() != EventStatus.DRAFT) {
      throw new ConflictException("Ticket types can only be added while event status is DRAFT");
    }

    String normalizedCurrency = currency.trim().toUpperCase();
    TicketType tt = new TicketType(eventId, name.trim(), priceMinor, normalizedCurrency, capacityTotal);
    return ticketTypeRepository.save(tt);
  }

  @Transactional(readOnly = true)
  public List<TicketType> listTicketTypes(UUID eventId) {
    return ticketTypeRepository.findByEventId(eventId);
  }
}
