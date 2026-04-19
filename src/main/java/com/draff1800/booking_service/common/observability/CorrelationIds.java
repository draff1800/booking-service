package com.draff1800.booking_service.common.observability;

import jakarta.servlet.http.HttpServletRequest;

public final class CorrelationIds {
  private CorrelationIds() {}

  public static String getIdFrom(HttpServletRequest request) {
    Object correlationId = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
    return correlationId != null ? correlationId.toString() : "unknown";
  }
}

