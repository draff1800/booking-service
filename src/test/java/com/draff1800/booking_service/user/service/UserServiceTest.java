package com.draff1800.booking_service.user.service;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.domain.UserRole;
import com.draff1800.booking_service.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  @Test
  void patchProfile_throwsConflict_whenHandleIsReserved() {
    // ARRANGE
    UUID userId = UUID.randomUUID();
    User user = new User(
        "test@example.com", 
        "HASHED", 
        UserRole.USER, 
        "old-handle", 
        "Old Display Name"
    );

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // ACT & ASSERT
    assertThatThrownBy(() -> userService.patchUser(userId, "admin", "New Display Name"))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Handle is reserved");
  }

  @Test
  void patchProfile_throwsConflict_whenHandleAlreadyTakenByAnotherUser() {
    // ARRANGE
    UUID currentUserId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();

    User currentUser = mock(User.class);
    when(currentUser.getId()).thenReturn(currentUserId);

    User otherUser = mock(User.class);
    when(otherUser.getId()).thenReturn(otherUserId);

    when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
    when(userRepository.findByHandle("taken-handle")).thenReturn(Optional.of(otherUser));


    // ACT & ASSERT
    assertThatThrownBy(() -> userService.patchUser(currentUserId, "taken handle", "null"))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Handle is already taken");
  }

  @Test
  void patchProfile_throwsConflict_whenDisplayNameIsBlank() {
    // ARRANGE
    UUID userId = UUID.randomUUID();
    User user = new User(
        "test@example.com", 
        "HASHED", 
        UserRole.USER, 
        "old-handle", 
        "Old Display Name"
    );

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // ACT & ASSERT
    assertThatThrownBy(() -> userService.patchUser(userId, "new-handle", " "))
        .isInstanceOf(ConflictException.class)
        .hasMessage("displayName must not be blank");
  }
}
