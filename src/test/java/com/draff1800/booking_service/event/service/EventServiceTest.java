package com.draff1800.booking_service.event.service;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.ForbiddenException;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.domain.EventStatus;
import com.draff1800.booking_service.event.repo.EventRepository;
import com.draff1800.booking_service.event.repo.TicketTypeRepository;
import com.draff1800.booking_service.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

  @Mock private EventRepository eventRepository;
  @Mock private UserRepository userRepository;
  @Mock private TicketTypeRepository ticketTypeRepository;

  @InjectMocks
  private EventService eventService;

  @Test
  void create_throwsConflict_whenEndsAtIsNotAfterStartsAt() {
    // ARRANGE
    Instant currentInstant = Instant.now();

    // ACT & ASSERT
    assertThatThrownBy(() ->
        eventService.create(
            "Test event",
            "desc",
            "venue",
            currentInstant,
            currentInstant,
            UUID.randomUUID(),
            null
        )
    )
        .isInstanceOf(ConflictException.class)
        .hasMessage("endsAt must be after startsAt");
  }

  @Test
  void publish_throwsForbidden_whenRequesterIsNotCreator() {
    // ARRANGE
    UUID eventId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    Event event = mock(Event.class);
    when(event.getCreatedBy()).thenReturn(creatorId);
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

    // ACT & ASSERT
    assertThatThrownBy(() -> eventService.publish(eventId, requesterId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the event creator can perform this action");

    verify(eventRepository, never()).save(any());
  }

  @Test
  void updateDetails_throwsConflict_whenEventIsNotDraft() {
    // ARRANGE
    UUID eventId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();

    Event event = mock(Event.class);
    when(event.getCreatedBy()).thenReturn(creatorId);
    when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

    // ACT & ASSERT
    assertThatThrownBy(() ->
        eventService.updateDetails(
            eventId,
            creatorId,
            "Updated",
            "desc",
            "venue",
            Instant.now().plusSeconds(1000),
            Instant.now().plusSeconds(2000)
        )
    )
        .isInstanceOf(ConflictException.class)
        .hasMessage("Updating event is only allowed while event is DRAFT");

    verify(eventRepository, never()).save(any());
  }
}
