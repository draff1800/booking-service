package com.draff1800.booking_service.admin.service;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.NotFoundException;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.domain.UserRole;
import com.draff1800.booking_service.user.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AdminUserService {

  private final UserRepository userRepository;

  public AdminUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public User updateRole(UUID userId, String rawRole) {
    User user = userRepository.findById(userId).orElseThrow(
        () -> new NotFoundException("User not found")
    );

    UserRole newRole;
    try {
      newRole = UserRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
    } catch (Exception e) {
      throw new ConflictException("Invalid role: " + rawRole);
    }

    user.setRole(newRole);
    return userRepository.save(user);
  }
}
