package com.draff1800.booking_service.user.service;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.NotFoundException;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.repo.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

  private static final Set<String> RESERVED_HANDLES = Set.of(
      "admin", "support", "api", "events", "users", "bookings", "auth", "me"
  );

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public User patchUser(UUID userId, String rawHandle, String rawDisplayName) {
    User user = userRepository.findById(userId).orElseThrow(
        () -> new NotFoundException("User not found")
    );

    boolean newHandle = rawHandle != null;
    boolean newDisplayName = rawDisplayName != null;

    if (!newHandle && !newDisplayName) {
      throw new ConflictException("At least one field must be provided");
    }

    if (newHandle) {
      String handle = normalizeHandle(rawHandle);
      validateHandle(handle, user.getId());
      user.setHandle(handle);
    }

    if (newDisplayName) {
      String displayName = rawDisplayName.trim();
      if (displayName.isBlank()) {
        throw new ConflictException("displayName must not be blank");
      }
      user.setDisplayName(displayName);
    }

    try {
      return userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Handle is already taken");
    }
  }

  private String normalizeHandle(String raw) {
    String handle = raw.trim().toLowerCase(Locale.ROOT);
    handle = handle.replaceAll("[^a-z0-9]+", "-");
    handle = handle.replaceAll("(^-|-$)", "");

    if (handle.isBlank()) {
      throw new ConflictException("Handle must contain letters or numbers");
    }

    return handle;
  }

  private void validateHandle(String handle, UUID currentUserId) {
    if (RESERVED_HANDLES.contains(handle)) {
      throw new ConflictException("Handle is reserved");
    }

    userRepository.findByHandle(handle).ifPresent(existing -> {
      if (!existing.getId().equals(currentUserId)) {
        throw new ConflictException("Handle is already taken");
      }
    });
  }
}
