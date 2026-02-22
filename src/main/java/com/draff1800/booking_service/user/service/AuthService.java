package com.draff1800.booking_service.user.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.draff1800.booking_service.common.error.exception.ConflictException;
import com.draff1800.booking_service.common.error.exception.UnauthorizedException;
import com.draff1800.booking_service.user.domain.User;
import com.draff1800.booking_service.user.domain.UserRole;
import com.draff1800.booking_service.user.repo.UserRepository;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String email, String rawPassword) {
        String normalisedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalisedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(normalisedEmail, hashedPassword, UserRole.USER);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        String normalisedEmail = email.trim().toLowerCase();
        UnauthorizedException loginException = new UnauthorizedException("Invalid credentials");

        User user = userRepository.findByEmail(normalisedEmail).orElseThrow(
            () -> loginException
        );

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw loginException;
        }

        return user;
    }
}
