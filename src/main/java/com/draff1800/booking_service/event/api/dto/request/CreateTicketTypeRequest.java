package com.draff1800.booking_service.event.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketTypeRequest(
      @NotBlank(message = "name is required")
      @Size(max = 100, message = "name must be less than 100 characters")
      String name,

      @NotNull(message = "priceMinor is required")
      @Min(value = 0, message = "priceMinor must be 0 or more")
      Integer priceMinor,

      @NotBlank(message = "currency is required")
      @Size(min = 3, max = 3, message = "currency must be a 3-letter code")
      String currency,

      @NotNull(message = "capacityTotal is required")
      @Min(value = 0, message = "capacityTotal must be 0 or more")
      Integer capacityTotal
  ) {}
