package com.draff1800.booking_service.common.resilience;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimitingFilterTest {

  @Test
  void doFilter_allowsRequestsWithinLimit() throws Exception {
    RateLimitingFilter filter = new RateLimitingFilter(objectMapper(), true, 1, 60);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicInteger calls = new AtomicInteger();

    filter.doFilter(request, response, countingChain(calls));

    assertThat(calls).hasValue(1);
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void doFilter_returnsTooManyRequests_whenLimitIsExceeded() throws Exception {
    RateLimitingFilter filter = new RateLimitingFilter(objectMapper(), true, 1, 60);
    AtomicInteger calls = new AtomicInteger();

    filter.doFilter(new MockHttpServletRequest("POST", "/bookings"), new MockHttpServletResponse(), countingChain(calls));

    MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
    filter.doFilter(new MockHttpServletRequest("POST", "/bookings"), limitedResponse, countingChain(calls));

    assertThat(calls).hasValue(1);
    assertThat(limitedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(limitedResponse.getHeader("Retry-After")).isEqualTo("60");
    assertThat(limitedResponse.getContentAsString()).contains("TOO_MANY_REQUESTS");
  }

  private FilterChain countingChain(AtomicInteger calls) {
    return (request, response) -> calls.incrementAndGet();
  }

  private ObjectMapper objectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }
}
