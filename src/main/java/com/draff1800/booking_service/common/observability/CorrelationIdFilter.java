package com.draff1800.booking_service.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";
  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
  private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    String correlationId = getCorrelationId(request);
    long calledAtNanoTime = System.nanoTime();

    request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
    response.setHeader(CORRELATION_ID_HEADER, correlationId);
    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      logRequest(request, response, calledAtNanoTime);
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }

  private String getCorrelationId(HttpServletRequest request) {
    String incomingCorrelationId = request.getHeader(CORRELATION_ID_HEADER);
    if (incomingCorrelationId != null && SAFE_CORRELATION_ID.matcher(incomingCorrelationId).matches()) {
      return incomingCorrelationId;
    }

    return UUID.randomUUID().toString();
  }

  private void logRequest(HttpServletRequest request, HttpServletResponse response, long calledAtNanoTime) {
    if (isNoiseEndpoint(request.getRequestURI())) {
      return;
    }

    long durationMs = (System.nanoTime() - calledAtNanoTime) / 1_000_000;

    logger.info(
      "http_request method={} path={} status={} durationMs={}",
      request.getMethod(),
      request.getRequestURI(),
      response.getStatus(),
      durationMs
    );
  }

  private boolean isNoiseEndpoint(String path) {
    return path.equals("/actuator/health")
      || path.startsWith("/actuator/health/")
      || path.equals("/actuator/prometheus");
  }
}
