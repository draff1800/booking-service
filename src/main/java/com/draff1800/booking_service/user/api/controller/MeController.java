package com.draff1800.booking_service.user.api.controller;

import com.draff1800.booking_service.security.jwt.AuthPrincipal;
import com.draff1800.booking_service.user.api.dto.response.UserResponse;
import com.draff1800.booking_service.user.api.mapper.UserResponseMapper;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final UserResponseMapper mapper;

    public MeController(UserResponseMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal authPrincipal) {
        return mapper.toResponse(authPrincipal);
    }
}
