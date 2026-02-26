package com.draff1800.booking_service.booking.service;

import com.draff1800.booking_service.booking.domain.Booking;
import com.draff1800.booking_service.booking.domain.BookingItem;
import com.draff1800.booking_service.booking.repo.BookingItemRepository;
import com.draff1800.booking_service.booking.repo.BookingRepository;
import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.NotFoundException;
import com.draff1800.booking_service.common.idempotency.IdempotencyKeys;
import com.draff1800.booking_service.event.domain.TicketType;
import com.draff1800.booking_service.event.repo.TicketTypeRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Service
public class BookingService {

  private final TicketTypeRepository ticketTypeRepository;
  private final BookingRepository bookingRepository;
  private final BookingItemRepository bookingItemRepository;

  public BookingService(
      TicketTypeRepository ticketTypeRepository,
      BookingRepository bookingRepository,
      BookingItemRepository bookingItemRepository
  ) {
    this.ticketTypeRepository = ticketTypeRepository;
    this.bookingRepository = bookingRepository;
    this.bookingItemRepository = bookingItemRepository;
  }

  public record BookingResult(Booking booking, List<BookingItem> items) {}

  @Transactional
  public BookingResult createBooking(
    UUID userId, 
    String idempotencyKey, 
    Map<UUID, Integer> quantitiesByTicketType
  ) {

    if (quantitiesByTicketType.isEmpty()) {
      throw new ConflictException("No booking items provided");
    }

    String normalisedIKey = IdempotencyKeys.normalize(idempotencyKey);

    var existingBooking = getExistingBooking(userId, normalisedIKey);
    if (existingBooking.isPresent()) {
      return existingBooking.get();
    }

    List<UUID> ticketTypeIds = new ArrayList<>(quantitiesByTicketType.keySet());
    List<TicketType> ticketTypes = ticketTypeRepository.findAllById(ticketTypeIds);

    if (ticketTypes.size() != ticketTypeIds.size()) {
      Set<UUID> found = ticketTypes.stream().map(TicketType::getId).collect(Collectors.toSet());
      List<UUID> missing = ticketTypeIds.stream().filter(id -> !found.contains(id)).toList();
      throw new NotFoundException("The following ticket type(s) were not found: " + missing);
    }

    for (Entry<UUID, Integer> entry : quantitiesByTicketType.entrySet()) {
      UUID ticketTypeId = entry.getKey();
      int quantity = entry.getValue();

      int numberOfTicketTypesUpdated = ticketTypeRepository.decrementCapacityIfAvailable(ticketTypeId, quantity);
      if (numberOfTicketTypesUpdated == 0) {
        throw new ConflictException("No tickets remaining for ticketTypeId=" + ticketTypeId);
      }
    }

    Booking booking;
    try {
      booking = bookingRepository.save(new Booking(userId, normalisedIKey));
    } catch (DataIntegrityViolationException exception) {
      // Re-check for existing booking in case the same request was made twice concurrently
      existingBooking = getExistingBooking(userId, normalisedIKey);
      if (existingBooking.isPresent()) {
        return existingBooking.get();
      }
      throw exception;
    }

    Map<UUID, TicketType> ticketTypesById = ticketTypes.stream().collect(
      Collectors.toMap(TicketType::getId, ticketType -> ticketType)
    );

    List<BookingItem> bookingItems = new ArrayList<>();
    for (Entry<UUID, Integer> entry : quantitiesByTicketType.entrySet()) {
      UUID ticketTypeId = entry.getKey();
      int quantity = entry.getValue();
      TicketType ticketType = ticketTypesById.get(ticketTypeId);

      BookingItem bookingItem = new BookingItem(
          booking.getId(),
          ticketTypeId,
          quantity,
          ticketType.getPriceMinor(),
          ticketType.getCurrency()
      );
      bookingItems.add(bookingItem);
    }

    bookingItems = bookingItemRepository.saveAll(bookingItems);

    return new BookingResult(booking, bookingItems);
  }

  @Transactional(readOnly = true)
  public Page<BookingResult> listMine(UUID userId, Pageable pageable) {
    Page<Booking> page = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

    List<UUID> bookingIds = page.getContent().stream()
      .map(Booking::getId)
      .toList();

    Map<UUID, List<BookingItem>> itemsByBookingId = bookingIds.isEmpty()
      ? Map.of()
      : bookingItemRepository.findByBookingIdIn(bookingIds).stream()
          .collect(
            Collectors.groupingBy(
              BookingItem::getBookingId
            )
          );

    return page.map(b -> new BookingResult(
        b,
        itemsByBookingId.getOrDefault(b.getId(), List.of())
    ));
  }

  private Optional<BookingResult> getExistingBooking(UUID userId, String idempotencyKey) {
    if (idempotencyKey == null) {
      return Optional.empty();
    }

    var existingBooking = bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

    if (existingBooking.isEmpty()) {
      return Optional.empty();
    }

    var bookingItems = bookingItemRepository.findByBookingId(existingBooking.get().getId());  

    return Optional.of(new BookingResult(existingBooking.get(), bookingItems));
  }
}
