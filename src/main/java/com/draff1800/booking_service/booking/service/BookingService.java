package com.draff1800.booking_service.booking.service;

import com.draff1800.booking_service.booking.domain.Booking;
import com.draff1800.booking_service.booking.domain.BookingItem;
import com.draff1800.booking_service.booking.repo.BookingItemRepository;
import com.draff1800.booking_service.booking.repo.BookingRepository;
import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.NotFoundException;
import com.draff1800.booking_service.common.idempotency.IdempotencyKeys;
import com.draff1800.booking_service.messaging.booking.BookingEventOutboxService;
import com.draff1800.booking_service.event.domain.TicketType;
import com.draff1800.booking_service.event.repo.TicketTypeRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.HexFormat;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Service
public class BookingService {

  private final TicketTypeRepository ticketTypeRepository;
  private final BookingRepository bookingRepository;
  private final BookingItemRepository bookingItemRepository;
  private final BookingEventOutboxService bookingEventOutboxService;

  public BookingService(
      TicketTypeRepository ticketTypeRepository,
      BookingRepository bookingRepository,
      BookingItemRepository bookingItemRepository,
      BookingEventOutboxService bookingEventOutboxService
  ) {
    this.ticketTypeRepository = ticketTypeRepository;
    this.bookingRepository = bookingRepository;
    this.bookingItemRepository = bookingItemRepository;
    this.bookingEventOutboxService = bookingEventOutboxService;
  }

  public record BookingWithItems(Booking booking, List<BookingItem> items) {}

  @Transactional
  public BookingWithItems createBooking(
    UUID userId, 
    String idempotencyKey, 
    Map<UUID, Integer> quantitiesByTicketType
  ) {

    if (quantitiesByTicketType.isEmpty()) {
      throw new ConflictException("No booking items provided");
    }

    validateQuantities(quantitiesByTicketType);

    String normalisedIKey = IdempotencyKeys.normalize(idempotencyKey);
    String requestFingerprint = getRequestFingerprint(normalisedIKey, quantitiesByTicketType);

    var existingBooking = getExistingBooking(userId, normalisedIKey, requestFingerprint);
    if (existingBooking.isPresent()) return existingBooking.get();

    List<UUID> ticketTypeIds = new ArrayList<>(quantitiesByTicketType.keySet());
    List<TicketType> ticketTypes = ticketTypeRepository.findAllById(ticketTypeIds);

    if (ticketTypes.size() != ticketTypeIds.size()) {
      Set<UUID> found = ticketTypes.stream().map(TicketType::getId).collect(Collectors.toSet());
      List<UUID> missing = ticketTypeIds.stream().filter(id -> !found.contains(id)).toList();
      throw new NotFoundException("The following ticket type(s) were not found: " + missing);
    }

    Booking booking;
    try {
      booking = bookingRepository.saveAndFlush(new Booking(userId, normalisedIKey, requestFingerprint));
    } catch (DataIntegrityViolationException exception) {
      // Re-check for an existing booking in case the same idempotent request arrived concurrently.
      existingBooking = getExistingBooking(userId, normalisedIKey, requestFingerprint);
      if (existingBooking.isPresent()) return existingBooking.get();
      throw exception;
    }

    List<Entry<UUID, Integer>> sortedQuantitiesByTicketType = quantitiesByTicketType.entrySet().stream()
      .sorted(Entry.comparingByKey())
      .toList();

    for (Entry<UUID, Integer> entry : sortedQuantitiesByTicketType) {
      UUID ticketTypeId = entry.getKey();
      int quantity = entry.getValue();

      int numberOfTicketTypesUpdated = ticketTypeRepository.decrementCapacityIfAvailable(ticketTypeId, quantity);
      if (numberOfTicketTypesUpdated == 0) {
        throw new ConflictException("No tickets remaining for ticketTypeId=" + ticketTypeId);
      }
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
    bookingEventOutboxService.enqueueBookingConfirmedEvent(booking, bookingItems);

    return new BookingWithItems(booking, bookingItems);
  }

  @Transactional(readOnly = true)
  public Page<BookingWithItems> listMine(UUID userId, Pageable pageable) {
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

    return page.map(booking -> new BookingWithItems(
        booking,
        itemsByBookingId.getOrDefault(booking.getId(), List.of())
    ));
  }

  private Optional<BookingWithItems> getExistingBooking(
      UUID userId,
      String idempotencyKey,
      String requestFingerprint
  ) {
    if (idempotencyKey == null) {
      return Optional.empty();
    }

    var possibleExistingBooking = bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

    if (possibleExistingBooking.isEmpty()) {
      return Optional.empty();
    }

    Booking existingBooking = possibleExistingBooking.get();
    if (existingBooking.getRequestFingerprint() != null && !Objects.equals(existingBooking.getRequestFingerprint(), requestFingerprint)) {
      throw new ConflictException("Idempotency-Key was already used for a different booking request");
    }

    var bookingItems = bookingItemRepository.findByBookingId(existingBooking.getId());

    return Optional.of(new BookingWithItems(existingBooking, bookingItems));
  }

  private void validateQuantities(Map<UUID, Integer> quantitiesByTicketType) {
    List<UUID> invalidTicketTypeIds = quantitiesByTicketType.entrySet().stream()
      .filter(entry -> entry.getValue() == null || entry.getValue() <= 0)
      .map(Entry::getKey)
      .toList();

    if (!invalidTicketTypeIds.isEmpty()) {
      throw new ConflictException("Booking quantities must be positive for ticketTypeIds=" + invalidTicketTypeIds);
    }
  }

  private String getRequestFingerprint(String idempotencyKey, Map<UUID, Integer> quantitiesByTicketType) {
    if (idempotencyKey == null) {
      return null;
    }

    String normalizedRequest = quantitiesByTicketType.entrySet().stream()
      .sorted(Entry.comparingByKey())
      .map(entry -> entry.getKey() + "=" + entry.getValue())
      .collect(Collectors.joining("\n"));

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(normalizedRequest.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 digest is unavailable", exception);
    }
  }
}
