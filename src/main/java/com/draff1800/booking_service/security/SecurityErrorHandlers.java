package com.draff1800.booking_service.security;

import com.draff1800.booking_service.common.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public SecurityErrorHandlers(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
    HttpServletRequest request, 
    HttpServletResponse response, 
    AuthenticationException ex
  ) throws IOException {
    write(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
  }

  @Override
  public void handle(
    HttpServletRequest request, 
    HttpServletResponse response, 
    AccessDeniedException ex
  ) throws IOException {
    write(response, request, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
  }

  private void write(
    HttpServletResponse response, 
    HttpServletRequest request, 
    HttpStatus status, 
    String error, 
    String message
  ) throws IOException {

    String traceId = UUID.randomUUID().toString();

    ApiError body = ApiError.of(
        status.value(),
        error,
        message,
        request.getRequestURI(),
        traceId,
        null
    );

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
