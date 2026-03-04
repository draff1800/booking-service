package com.draff1800.booking_service.event.service;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.ForbiddenException;
import com.draff1800.booking_service.common.error.exception.NotFoundException;
import com.draff1800.booking_service.common.idempotency.IdempotencyKeys;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.domain.EventStatus;
import com.draff1800.booking_service.event.domain.TicketType;
import com.draff1800.booking_service.event.repo.EventRepository;
import com.draff1800.booking_service.event.repo.TicketTypeRepository;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.repo.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

  private final EventRepository eventRepository;
  private final UserRepository userRepository;
  private final TicketTypeRepository ticketTypeRepository;

  public EventService(
    EventRepository eventRepository,
    UserRepository userRepository,
    TicketTypeRepository ticketTypeRepository
  ) {
    this.eventRepository = eventRepository;
    this.userRepository = userRepository;
    this.ticketTypeRepository = ticketTypeRepository;
  }

  public record EventWithOrganizer(Event event, @Nullable User organizer) {}

  @Transactional
  public Event create(
    String title, 
    String description, 
    String venue, 
    Instant startsAt, 
    Instant endsAt, 
    UUID createdBy,
    String idempotencyKey
  ) {

    String normalisedIKey = IdempotencyKeys.normalize(idempotencyKey);

    var existingEvent = getExistingEvent(createdBy, normalisedIKey);
    if (existingEvent.isPresent()) return existingEvent.get();

    assertValidEventTimes(startsAt, endsAt);

    Event event;
    try {
      event = eventRepository.save(
        new Event(title, description, venue, startsAt, endsAt, createdBy, normalisedIKey)
      );
    } catch(DataIntegrityViolationException exception) {
      // Re-check for existing event in case same request was made twice concurrently
      existingEvent = getExistingEvent(createdBy, normalisedIKey);
      if (existingEvent.isPresent()) return existingEvent.get();
      throw exception;
    }

    return event;
  }

  @Transactional(readOnly = true)
  public EventWithOrganizer get(UUID id) {
    Event event = eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));

    User organizer = userRepository.findById(event.getCreatedBy()).orElse(null);

    return new EventWithOrganizer(event, organizer);
  }

  @Transactional(readOnly = true)
  public Page<EventWithOrganizer> listMine(UUID userId, Pageable pageable) {
    Page<Event> page = eventRepository.findByCreatedByAndStatusInOrderByStartsAtAsc(
        userId,
        List.of(EventStatus.DRAFT, EventStatus.PUBLISHED),
        pageable
    );

    User me = userRepository.findById(userId).orElse(null);
    
    return page.map(event -> new EventWithOrganizer(event, me));
  }

  @Transactional(readOnly = true)
  public Page<EventWithOrganizer> listPublicUpcoming(Pageable pageable) {
    Page<Event> page = eventRepository.findByStatusAndStartsAtAfterOrderByStartsAtAsc(
      EventStatus.PUBLISHED, 
      Instant.now(), 
      pageable
    );

    Map<UUID, User> organizersByUserId = organizersByUserId(page.getContent());

    return page.map(
      event -> new EventWithOrganizer(
        event, 
        organizersByUserId.get(
          event.getCreatedBy()
        )
      )
    );
  }

  @Transactional(readOnly = true)
  public EventWithOrganizer wrapWithOrganizer(Event event) {
    User organizer = userRepository.findById(event.getCreatedBy()).orElse(null);
    return new EventWithOrganizer(event, organizer);
  }

  @Transactional
  public Event publish(UUID eventId, UUID requesterUserId) {
    Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));

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

    Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));

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
    Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));

    assertUserIsEventCreator(event, requesterUserId);

    if (event.getStatus() == EventStatus.CANCELLED) {
      return event;
    }

    event.cancel();
    return eventRepository.save(event);
  }

  private void assertValidEventTimes(Instant startsAt, Instant endsAt) {
    if (!endsAt.isAfter(startsAt)) {
      throw new ConflictException("endsAt must be after startsAt");
    }
  }

  @Transactional(readOnly = true)
  private Event getEvent(UUID id) {
    return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
  }

  private Map<UUID, User> organizersByUserId(List<Event> events) {
    List<UUID> creatorIds = events.stream()
      .map(Event::getCreatedBy)
      .distinct()
      .toList();

    if (creatorIds.isEmpty()) return Map.of();

    List<User> users = userRepository.findByIdIn(creatorIds);

    return users.stream().collect(
      Collectors.toMap(
        User::getId, 
        user -> user
      )
    );
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

  private Optional<Event> getExistingEvent(UUID createdBy, String idempotencyKey) {
    if (idempotencyKey == null) {
      return Optional.empty();
    }

    var existingEvent = eventRepository.findByCreatedByAndIdempotencyKey(createdBy, idempotencyKey);

    if (existingEvent.isEmpty()) {
      return Optional.empty();
    } 

    return existingEvent;
  }

  @Transactional
  public TicketType addTicketType(
    UUID eventId, 
    UUID requesterUserId, 
    String name, 
    int priceMinor, 
    String currency, 
    int capacityTotal,
    String idempotencyKey
  ) {

    Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));

    assertUserIsEventCreator(event, requesterUserId);
    assertEventIsDraft(event, "Adding ticket types");

    String normalizedIKey = IdempotencyKeys.normalize(idempotencyKey);

    var existingTicketType = getExistingTicketType(eventId, normalizedIKey);
    if (existingTicketType.isPresent()) return existingTicketType.get();

    String normalizedName = name.trim();
    String normalizedCurrency = currency.trim().toUpperCase();

    TicketType ticketType;
    try {
      ticketType = ticketTypeRepository.save(
        new TicketType(eventId, normalizedName, priceMinor, normalizedCurrency, capacityTotal, normalizedIKey)
      );
    } catch (DataIntegrityViolationException exception) {
      // Re-check for existing ticket-type in case same request was made twice concurrently
      existingTicketType = getExistingTicketType(eventId, normalizedIKey);
      if (existingTicketType.isPresent()) return existingTicketType.get();
      throw exception;
    }

    return ticketType;
  }

  @Transactional(readOnly = true)
  public List<TicketType> listTicketTypes(UUID eventId) {
    return ticketTypeRepository.findByEventId(eventId);
  }

  private Optional<TicketType> getExistingTicketType(UUID eventId, String idempotencyKey) {
    if (idempotencyKey == null) {
      return Optional.empty();
    }

    var existingTicketType = ticketTypeRepository.findByEventIdAndIdempotencyKey(eventId, idempotencyKey);

    if (existingTicketType.isEmpty()) {
      return Optional.empty();
    } 

    return existingTicketType;
  }
}
