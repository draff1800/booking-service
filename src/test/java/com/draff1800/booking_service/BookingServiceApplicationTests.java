package com.draff1800.booking_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookingServiceApplicationTests {

	@Autowired
	MockMvc mockMvc;

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

	@Test
	void contextLoads() {}

	@Test
	void actuatorHealthIsPublicAndReturnsCorrelationId() throws Exception {
		mockMvc.perform(get("/actuator/health").header("X-Correlation-ID", "test-correlation-id"))
			.andExpect(status().isOk())
			.andExpect(header().string("X-Correlation-ID", "test-correlation-id"));
	}

	@Test
	void actuatorMetricsRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/metrics"))
			.andExpect(status().isUnauthorized());
	}
}
