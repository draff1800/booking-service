package com.draff1800.booking_service.user.api;

import com.draff1800.booking_service.user.api.dto.request.LoginRequest;
import com.draff1800.booking_service.user.api.dto.request.RegisterRequest;
import com.draff1800.booking_service.user.api.dto.response.UserResponse;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email(), request.password());
        return new UserResponse(user.getId().toString(), user.getEmail(), user.getRole().name());
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        return new UserResponse(user.getId().toString(), user.getEmail(), user.getRole().name());
    }
}
