package com.draff1800.booking_service.common.idempotency;

public final class IdempotencyKeys {
  private IdempotencyKeys() {}

  public static String normalize(String key) {
    if (key == null) return null;
    String normalizedKey = key.trim();
    return normalizedKey.isEmpty() ? null : normalizedKey;
  }
}
