package com.draff1800.booking_service.event.repo;

import com.draff1800.booking_service.event.domain.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
  List<TicketType> findByEventId(UUID eventId);

  @Modifying
  @Query("""
    update TicketType ticketType
    set ticketType.capacityRemaining = ticketType.capacityRemaining - :qty
    where ticketType.id = :ticketTypeId and ticketType.capacityRemaining >= :qty
  """)
  int decrementCapacityIfAvailable(@Param("ticketTypeId") UUID ticketTypeId, @Param("qty") int qty);
}
