package com.draff1800.booking_service.admin.api;

import com.draff1800.booking_service.user.api.dto.response.UserResponse;
import com.draff1800.booking_service.user.api.mapper.UserResponseMapper;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.admin.api.dto.request.UpdateUserRoleRequest;
import com.draff1800.booking_service.admin.service.AdminUsersService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class AdminUsersController {

  private final AdminUsersService adminUserService;
  private final UserResponseMapper mapper;

  public AdminUsersController(AdminUsersService adminUserService, UserResponseMapper mapper) {
    this.adminUserService = adminUserService;
    this.mapper = mapper;
  }

  @PatchMapping("/{id}/role")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateRole(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateUserRoleRequest req
  ) {

    User updatedUser = adminUserService.updateRole(id, req.role());
    return mapper.toResponse(updatedUser);
  }
}
