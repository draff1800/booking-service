package com.draff1800.booking_service.user.api.controller;

import com.draff1800.booking_service.security.jwt.AuthPrincipal;
import com.draff1800.booking_service.user.api.dto.request.PatchUserRequest;
import com.draff1800.booking_service.user.api.dto.response.UserResponse;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PatchMapping("/me")
  public UserResponse patchMe(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody PatchUserRequest req
  ) {
    User updated = userService.patchUser(
        principal.userId(),
        req.displayName(),
        req.handle()
    );

    return new UserResponse(
        updated.getId().toString(),
        updated.getEmail(),
        updated.getRole().name(),
        updated.getHandle(),
        updated.getDisplayName()
    );
  }
}
