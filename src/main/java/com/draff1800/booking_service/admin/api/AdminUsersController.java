package com.draff1800.booking_service.admin.api;

import com.draff1800.booking_service.common.error.exception.ForbiddenException;
import com.draff1800.booking_service.security.jwt.AuthPrincipal;
import com.draff1800.booking_service.user.api.dto.response.UserResponse;
import com.draff1800.booking_service.user.api.mapper.UserResponseMapper;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.admin.api.dto.request.UpdateUserRoleRequest;
import com.draff1800.booking_service.admin.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class AdminUsersController {

  private final AdminUserService adminUserService;
  private final UserResponseMapper mapper;

  public AdminUsersController(AdminUserService adminUserService, UserResponseMapper mapper) {
    this.adminUserService = adminUserService;
    this.mapper = mapper;
  }

  @PatchMapping("/{id}/role")
  public UserResponse updateRole(
      @PathVariable UUID id,
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody UpdateUserRoleRequest req
  ) {

    if (!"ADMIN".equals(principal.role())) {
      throw new ForbiddenException("Admin only");
    }

    User updatedUser = adminUserService.updateRole(id, req.role());
    return mapper.toResponse(updatedUser);
  }
}
