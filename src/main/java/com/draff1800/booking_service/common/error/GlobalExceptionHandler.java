package com.draff1800.booking_service.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.UnauthorizedException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, Object> fieldErrors = new LinkedHashMap<>();
    for (FieldError fe : exception.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
    }

    return ResponseEntity.badRequest().body(ApiError.of(
        400,
        "VALIDATION_ERROR",
        "Request validation failed",
        request.getRequestURI(),
        randomTraceId(),
        Map.of("fields", fieldErrors)
    ));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(ApiError.of(
        400,
        "VALIDATION_ERROR",
        exception.getMessage(),
        request.getRequestURI(),
        randomTraceId(),
        null
    ));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(ConflictException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
      409,
      "CONFLICT",
      exception.getMessage(),
      request.getRequestURI(),
      randomTraceId(),
      null
    ));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(
      401,
      "UNAUTHORIZED",
      exception.getMessage(),
      request.getRequestURI(),
      randomTraceId(),
      null
    ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception exception, HttpServletRequest request) {
    String traceId = randomTraceId();
    String requestUri = request.getRequestURI();
    
    logger.error("Unexpected Error - traceId={} path={}", traceId, requestUri, exception);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
        500,
        "INTERNAL_ERROR",
        "Unexpected error",
        requestUri,
        randomTraceId(),
        null
    ));
  }

  private String randomTraceId() {
    return UUID.randomUUID().toString();
  }
}

