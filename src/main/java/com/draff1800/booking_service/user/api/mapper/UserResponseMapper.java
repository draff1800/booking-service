package com.draff1800.booking_service.user.api.mapper;

import org.springframework.stereotype.Component;

import com.draff1800.booking_service.security.jwt.AuthPrincipal;
import com.draff1800.booking_service.user.api.dto.response.UserResponse;
import com.draff1800.booking_service.user.domain.User;

@Component
public class UserResponseMapper {

  public UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId().toString(),
        user.getEmail(),
        user.getRole().name()
    );
  }

  public UserResponse toResponse(AuthPrincipal authPrincipal) {
    return new UserResponse(
        authPrincipal.userId().toString(),
        authPrincipal.email(),
        authPrincipal.role()
    );
  }
}
