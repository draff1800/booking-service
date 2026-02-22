package com.draff1800.booking_service.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.draff1800.booking_service.user.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long timeToLiveSeconds;

    public JwtService(
        @Value("${JWT_SECRET}") String secret,
        @Value("${JWT_TIME_TO_LIVE_SECONDS") long timeToLiveSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    public String issueToken(User user) {
        Instant currentInstant = Instant.now();
        Instant expiryInstant = currentInstant.plusSeconds(timeToLiveSeconds);

        return Jwts.builder()
            .subject(user.getId().toString())
            .issuedAt(Date.from(currentInstant))
            .expiration(Date.from(expiryInstant))
            .claim("Email", user.getEmail())
            .claim("Role", user.getRole().name())
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UUID subjectAsUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }
}
