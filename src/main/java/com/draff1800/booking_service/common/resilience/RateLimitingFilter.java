package com.draff1800.booking_service.common.resilience;

import com.draff1800.booking_service.common.error.ApiError;
import com.draff1800.booking_service.common.observability.CorrelationIds;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

  private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;
  private final boolean enabled;
  private final int capacity;
  private final Duration refillPeriod;

  public RateLimitingFilter(
      ObjectMapper objectMapper,
      @Value("${app.rate-limit.enabled:true}") boolean enabled,
      @Value("${app.rate-limit.capacity:20}") int capacity,
      @Value("${app.rate-limit.refill-period-seconds:60}") long refillPeriodSeconds
  ) {
    this.objectMapper = objectMapper;
    this.enabled = enabled;
    this.capacity = capacity;
    this.refillPeriod = Duration.ofSeconds(refillPeriodSeconds);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    if (!enabled || !isLimitedRoute(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    TokenBucket bucket = buckets.computeIfAbsent(clientKey(request), ignored -> new TokenBucket(capacity));
    if (bucket.tryConsume(refillPeriod)) {
      filterChain.doFilter(request, response);
      return;
    }

    writeRateLimitResponse(request, response);
  }

  private boolean isLimitedRoute(HttpServletRequest request) {
    if (!HttpMethod.POST.matches(request.getMethod())) {
      return false;
    }

    String path = request.getRequestURI();
    return path.equals("/auth/login")
      || path.equals("/auth/register")
      || path.equals("/bookings");
  }

  private String clientKey(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null && !authorization.isBlank()) {
      return request.getRequestURI() + ":auth:" + authorization.hashCode();
    }

    return request.getRequestURI() + ":ip:" + request.getRemoteAddr();
  }

  private void writeRateLimitResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(refillPeriod.toSeconds()));

    ApiError error = ApiError.of(
      429,
      "TOO_MANY_REQUESTS",
      "Too many requests. Please retry later.",
      request.getRequestURI(),
      CorrelationIds.getIdFrom(request),
      Map.of("waitSecondsUntilRetry", refillPeriod.toSeconds())
    );

    objectMapper.writeValue(response.getWriter(), error);
  }

  private static final class TokenBucket {
    private final int capacity;
    private int tokens;
    private long lastRefillNanoTime;

    private TokenBucket(int capacity) {
      this.capacity = capacity;
      this.tokens = capacity;
      this.lastRefillNanoTime = System.nanoTime();
    }

    private synchronized boolean tryConsume(Duration refillPeriod) {
      long currentNanoTime = System.nanoTime();
      if (currentNanoTime - lastRefillNanoTime >= refillPeriod.toNanos()) {
        tokens = capacity;
        lastRefillNanoTime = currentNanoTime;
      }

      if (tokens == 0) {
        return false;
      }

      tokens--;
      return true;
    }
  }
}
