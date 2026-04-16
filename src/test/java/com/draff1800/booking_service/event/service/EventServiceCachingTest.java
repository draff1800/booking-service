package com.draff1800.booking_service.event.service;

import com.draff1800.booking_service.config.CacheConfig;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.domain.EventStatus;
import com.draff1800.booking_service.event.repo.EventRepository;
import com.draff1800.booking_service.event.repo.TicketTypeRepository;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.domain.UserRole;
import com.draff1800.booking_service.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EventServiceCachingTest.TestConfig.class)
class EventServiceCachingTest {

  @Autowired
  private EventService eventService;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CacheManager cacheManager;

  @Test
  void get_usesCacheUntilEventIsMutated() {
    UUID eventId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();

    Event event = mock(Event.class);
    when(event.getId()).thenReturn(eventId);
    when(event.getCreatedBy()).thenReturn(creatorId);
    when(event.getStatus()).thenReturn(EventStatus.DRAFT);

    User organizer = new User(
      "organizer@example.com",
      "hashed-password",
      UserRole.USER,
      "organizer",
      "Organizer"
    );

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRepository.save(event)).thenReturn(event);
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(organizer));

    EventService.EventWithOrganizer firstGet = eventService.get(eventId);
    EventService.EventWithOrganizer secondGet = eventService.get(eventId);

    assertThat(firstGet).isEqualTo(secondGet);
    assertThat(cacheManager.getCache(CacheConfig.EVENT_DETAIL_CACHE).get(eventId)).isNotNull();

    verify(eventRepository, times(1)).findById(eventId);
    verify(userRepository, times(1)).findById(creatorId);

    eventService.publish(eventId, creatorId);
    EventService.EventWithOrganizer thirdGet = eventService.get(eventId);

    assertThat(thirdGet.event()).isEqualTo(event);
    verify(eventRepository, times(3)).findById(eventId);
    verify(userRepository, times(2)).findById(creatorId);
  }

  @Configuration
  @Import({CacheConfig.class, EventService.class})
  static class TestConfig {

    @Bean
    EventRepository eventRepository() {
      return mock(EventRepository.class);
    }

    @Bean
    UserRepository userRepository() {
      return mock(UserRepository.class);
    }

    @Bean
    TicketTypeRepository ticketTypeRepository() {
      return mock(TicketTypeRepository.class);
    }
  }
}
