package com.draff1800.booking_service.user.service;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.UnauthorizedException;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.domain.UserRole;
import com.draff1800.booking_service.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private BCryptPasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthService authService;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  @Test
  void register_throwsConflict_whenEmailAlreadyExists() {
    // ARRANGE
    when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

    // ACT
    assertThatThrownBy(() -> authService.register("test@example.com", "password123"))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Email is already registered");

    // ASSERT
    verify(userRepository, never()).save(any());
  }

  @Test
  void register_hashesPassword_andNormalizesEmail_beforeSave() {
    // ARRANGE
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(userRepository.existsByHandle(anyString())).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("HASHED");

    // ACT
    authService.register(" Test@Example.com ", "password123");

    // ASSERT
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    assertThat(savedUser.getPasswordHash()).isEqualTo("HASHED");
    assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
    assertThat(savedUser.getHandle()).isNotBlank();
    assertThat(savedUser.getDisplayName()).isNotBlank();
  }

  @Test
  void login_throwsUnauthorized_whenPasswordDoesNotMatch() {
    // ARRANGE
    User existingUser = mock(User.class);
    when(existingUser.getPasswordHash()).thenReturn("HASHED");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
    when(passwordEncoder.matches("wrong-password", "HASHED")).thenReturn(false);

    // ACT & ASSERT
    assertThatThrownBy(() -> authService.login("test@example.com", "wrong-password"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage("Invalid credentials");
  }
}
