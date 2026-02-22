package com.draff1800.booking_service.user.api.controller;

import com.draff1800.booking_service.security.jwt.JwtService;
import com.draff1800.booking_service.user.api.dto.request.LoginRequest;
import com.draff1800.booking_service.user.api.dto.request.RegisterRequest;
import com.draff1800.booking_service.user.api.dto.response.AuthResponse;
import com.draff1800.booking_service.user.api.dto.response.UserResponse;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email(), request.password());
        String jwtToken = jwtService.issueToken(user);
        UserResponse userResponse = new UserResponse(user.getId().toString(), user.getEmail(), user.getRole().name());

        return new AuthResponse(jwtToken, userResponse);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        String jwtToken = jwtService.issueToken(user);
        UserResponse userResponse = new UserResponse(user.getId().toString(), user.getEmail(), user.getRole().name());

        return new AuthResponse(jwtToken, userResponse);
    }
}
