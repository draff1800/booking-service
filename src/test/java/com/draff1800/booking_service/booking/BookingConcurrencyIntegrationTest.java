package com.draff1800.booking_service.booking;

import com.draff1800.booking_service.booking.domain.BookingItem;
import com.draff1800.booking_service.booking.repo.BookingItemRepository;
import com.draff1800.booking_service.booking.repo.BookingRepository;
import com.draff1800.booking_service.booking.service.BookingService;
import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.event.domain.Event;
import com.draff1800.booking_service.event.domain.TicketType;
import com.draff1800.booking_service.event.repo.EventRepository;
import com.draff1800.booking_service.event.repo.TicketTypeRepository;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.domain.UserRole;
import com.draff1800.booking_service.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class BookingConcurrencyIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

    registry.add("JWT_SECRET", () -> "test-secret-that-is-long-enough-for-hs256-signing-123456");
    registry.add("JWT_TIME_TO_LIVE_SECONDS", () -> "900");
  }

  @Autowired
  private BookingService bookingService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private TicketTypeRepository ticketTypeRepository;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private BookingItemRepository bookingItemRepository;

  @BeforeEach
  void cleanDatabase() {
    bookingItemRepository.deleteAll();
    bookingRepository.deleteAll();
    ticketTypeRepository.deleteAll();
    eventRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void concurrentBookings_doNotOversell() throws Exception {
    // ARRANGE
    int capacity = 10;
    int attempts = 50;

    User organizer = userRepository.save(
        new User(
            "organizer@example.com",
            "hashed-password",
            UserRole.USER,
            "organizer",
            "Organizer"
        )
    );

    User buyer = userRepository.save(
        new User(
            "buyer@example.com",
            "hashed-password",
            UserRole.USER,
            "buyer",
            "Buyer"
        )
    );

    Event event = eventRepository.save(
        new Event(
            "Concurrency Test Event",
            "Tests oversell prevention",
            "London",
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(7200),
            organizer.getId(),
            null
        )
    );

    TicketType ticketType = ticketTypeRepository.save(
        new TicketType(
            event.getId(),
            "General Admission",
            2500,
            "GBP",
            capacity,
            null
        )
    );

    UUID buyerId = buyer.getId();
    UUID ticketTypeId = ticketType.getId();

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger conflictCount = new AtomicInteger(0);
    AtomicInteger unexpectedFailureCount = new AtomicInteger(0);

    ExecutorService executor = Executors.newFixedThreadPool(attempts);
    CountDownLatch ready = new CountDownLatch(attempts);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(attempts);

    // ACT
    try {
      for (int i = 0; i < attempts; i++) {
        executor.submit(() -> {
          ready.countDown();
          try {
            start.await();

            bookingService.createBooking(
                buyerId,
                null,
                Map.of(ticketTypeId, 1)
            );

            successCount.incrementAndGet();
          } catch (ConflictException e) {
            conflictCount.incrementAndGet();
          } catch (Exception e) {
            unexpectedFailureCount.incrementAndGet();
          } finally {
            done.countDown();
          }
          return null;
        });
      }

      ready.await(10, TimeUnit.SECONDS);
      start.countDown();
      done.await(30, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    // ASSERT
    TicketType refreshedTicketType = ticketTypeRepository.findById(ticketTypeId).orElseThrow();

    assertThat(successCount.get()).isEqualTo(capacity);
    assertThat(conflictCount.get()).isEqualTo(attempts - capacity);
    assertThat(unexpectedFailureCount.get()).isZero();

    assertThat(refreshedTicketType.getCapacityRemaining()).isZero();
    assertThat(bookingRepository.count()).isEqualTo(capacity);

    List<BookingItem> bookingItems = bookingItemRepository.findAll();
    int totalBookedQuantity = bookingItems.stream()
        .mapToInt(BookingItem::getQuantity)
        .sum();

    assertThat(totalBookedQuantity).isEqualTo(capacity);
  }
}
