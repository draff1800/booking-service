package com.draff1800.booking_service.booking.service;

import com.draff1800.booking_service.booking.domain.Booking;
import com.draff1800.booking_service.booking.domain.BookingItem;
import com.draff1800.booking_service.booking.repo.BookingItemRepository;
import com.draff1800.booking_service.booking.repo.BookingRepository;
import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.NotFoundException;
import com.draff1800.booking_service.event.domain.TicketType;
import com.draff1800.booking_service.event.repo.TicketTypeRepository;
import com.draff1800.booking_service.messaging.booking.BookingEventOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

  @Mock private TicketTypeRepository ticketTypeRepository;
  @Mock private BookingRepository bookingRepository;
  @Mock private BookingItemRepository bookingItemRepository;
  @Mock private BookingEventOutboxService bookingEventOutboxService;

  @InjectMocks
  private BookingService bookingService;

  @Test
  void createBooking_throwsConflict_whenQuantityIsNotPositive() {
    // ARRANGE
    UUID userId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();

    // ACT & ASSERT
    assertThatThrownBy(() ->
        bookingService.createBooking(userId, null, Map.of(ticketTypeId, 0))
    )
        .isInstanceOf(ConflictException.class)
        .hasMessage("Booking quantities must be positive for ticketTypeIds=[" + ticketTypeId + "]");

    verifyNoInteractions(ticketTypeRepository, bookingRepository, bookingItemRepository);
  }

  @Test
  void createBooking_throwsConflict_whenNoCapacityRemaining() {
    // ARRANGE
    UUID userId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();

    TicketType ticketType = mock(TicketType.class);
    when(ticketTypeRepository.findAllById(List.of(ticketTypeId))).thenReturn(List.of(ticketType));
    when(ticketTypeRepository.decrementCapacityIfAvailable(ticketTypeId, 1)).thenReturn(0);

    // ACT & ASSERT
    assertThatThrownBy(() ->
        bookingService.createBooking(userId, null, Map.of(ticketTypeId, 1))
    )
        .isInstanceOf(ConflictException.class)
        .hasMessage("No tickets remaining for ticketTypeId=" + ticketTypeId);

    verify(bookingRepository, never()).save(any());
  }

  @Test
  void createBooking_throwsNotFound_whenTicketTypeDoesNotExist() {
    // ARRANGE
    UUID userId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();

    when(ticketTypeRepository.findAllById(List.of(ticketTypeId))).thenReturn(List.of());

    // ACT & ASSERT
    assertThatThrownBy(() ->
        bookingService.createBooking(userId, null, Map.of(ticketTypeId, 1))
    )
        .isInstanceOf(NotFoundException.class)
        .hasMessage("The following ticket type(s) were not found: [" + ticketTypeId + "]");
  }

  @Test
  void createBooking_savesBookingAndItems_whenAllChecksPass() {
    // ARRANGE
    UUID userId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();

    TicketType ticketType = mock(TicketType.class);
    when(ticketType.getId()).thenReturn(ticketTypeId);
    when(ticketType.getPriceMinor()).thenReturn(2500);
    when(ticketType.getCurrency()).thenReturn("GBP");

    when(ticketTypeRepository.findAllById(List.of(ticketTypeId))).thenReturn(List.of(ticketType));
    when(ticketTypeRepository.decrementCapacityIfAvailable(ticketTypeId, 1)).thenReturn(1);

    Booking booking = mock(Booking.class);
    UUID bookingId = UUID.randomUUID();
    when(booking.getId()).thenReturn(bookingId);

    when(bookingRepository.save(any())).thenReturn(booking);
    when(bookingItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // ACT
    BookingService.BookingWithItems bookingWithItems =
        bookingService.createBooking(userId, null, Map.of(ticketTypeId, 1));

    // ASSERT
    assertThat(bookingWithItems.booking()).isEqualTo(booking);
    assertThat(bookingWithItems.items()).hasSize(1);

    verify(bookingRepository).save(any());
    verify(bookingItemRepository).saveAll(any());
    verify(bookingEventOutboxService).enqueueBookingConfirmedEvent(eq(booking), anyList());
  }

  @Test
  void createBooking_reservesTicketTypesInStableOrder() {
    // ARRANGE
    UUID userId = UUID.randomUUID();
    UUID ticketType1Id = UUID.fromString("00000000-0000-0000-0000-0000000001aa");
    UUID ticketType2Id = UUID.fromString("00000000-0000-0000-0000-0000000002bb");

    TicketType ticketType1 = mock(TicketType.class);
    when(ticketType1.getId()).thenReturn(ticketType1Id);
    when(ticketType1.getPriceMinor()).thenReturn(2500);
    when(ticketType1.getCurrency()).thenReturn("GBP");

    TicketType ticketType2 = mock(TicketType.class);
    when(ticketType2.getId()).thenReturn(ticketType2Id);
    when(ticketType2.getPriceMinor()).thenReturn(3500);
    when(ticketType2.getCurrency()).thenReturn("GBP");

    when(ticketTypeRepository.findAllById(any()))
        .thenReturn(List.of(ticketType2, ticketType1));
    when(ticketTypeRepository.decrementCapacityIfAvailable(any(), anyInt())).thenReturn(1);

    Booking booking = mock(Booking.class);
    when(booking.getId()).thenReturn(UUID.randomUUID());

    when(bookingRepository.save(any())).thenReturn(booking);
    when(bookingItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // ACT
    bookingService.createBooking(userId, null, Map.of(ticketType2Id, 1, ticketType1Id, 1));

    // ASSERT
    InOrder inOrder = inOrder(ticketTypeRepository);
    inOrder.verify(ticketTypeRepository).decrementCapacityIfAvailable(ticketType1Id, 1);
    inOrder.verify(ticketTypeRepository).decrementCapacityIfAvailable(ticketType2Id, 1);
    verify(bookingEventOutboxService).enqueueBookingConfirmedEvent(eq(booking), anyList());
  }

  @Test
  void listMine_returnsBookingsWithItems() {
    // ARRANGE
    UUID userId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);

    UUID booking1Id = UUID.randomUUID();
    UUID booking2Id = UUID.randomUUID();

    Booking booking1 = mock(Booking.class);
    Booking booking2 = mock(Booking.class);
    when(booking1.getId()).thenReturn(booking1Id);
    when(booking2.getId()).thenReturn(booking2Id);

    Page<Booking> bookingPage = new PageImpl<>(List.of(booking1, booking2), pageable, 2);

    BookingItem item1 = mock(BookingItem.class);
    BookingItem item2 = mock(BookingItem.class);
    BookingItem item3 = mock(BookingItem.class);
    when(item1.getBookingId()).thenReturn(booking1Id);
    when(item2.getBookingId()).thenReturn(booking1Id);
    when(item3.getBookingId()).thenReturn(booking2Id);

    when(bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(bookingPage);
    when(bookingItemRepository.findByBookingIdIn(List.of(booking1Id, booking2Id)))
        .thenReturn(List.of(item1, item2, item3));

    // ACT
    Page<BookingService.BookingWithItems> result = bookingService.listMine(userId, pageable);

    // ASSERT
    assertThat(result.getContent()).hasSize(2);

    BookingService.BookingWithItems result1 = result.getContent().get(0);
    BookingService.BookingWithItems result2 = result.getContent().get(1);

    assertThat(result1.booking()).isEqualTo(booking1);
    assertThat(result1.items()).containsExactly(item1, item2);

    assertThat(result2.booking()).isEqualTo(booking2);
    assertThat(result2.items()).containsExactly(item3);

    verify(bookingRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    verify(bookingItemRepository).findByBookingIdIn(List.of(booking1Id, booking2Id));
  }  
}
