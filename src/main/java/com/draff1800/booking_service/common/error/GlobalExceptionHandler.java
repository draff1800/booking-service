package com.draff1800.booking_service.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    Map<String, Object> fieldErrors = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
    }

    return ResponseEntity.badRequest().body(ApiError.of(
        400,
        "VALIDATION_ERROR",
        "Request validation failed",
        req.getRequestURI(),
        randomTraceId(),
        Map.of("fields", fieldErrors)
    ));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
    return ResponseEntity.badRequest().body(ApiError.of(
        400,
        "VALIDATION_ERROR",
        ex.getMessage(),
        req.getRequestURI(),
        randomTraceId(),
        null
    ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
        500,
        "INTERNAL_ERROR",
        "Unexpected error",
        req.getRequestURI(),
        randomTraceId(),
        null
    ));
  }

  private String randomTraceId() {
    return UUID.randomUUID().toString();
  }
}

