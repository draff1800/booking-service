package com.draff1800.booking_service.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(name = "handle", length = 50)
    private String handle;

    @Column(name = "display_name", length = 80)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {}

    public User(
        String email, 
        String passwordHash, 
        UserRole role, 
        String handle, 
        String displayName
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.handle = handle;
        this.displayName = displayName;
    }

    @PrePersist
    void prePersist() {
        Instant currentInstant = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (role == null) role = UserRole.USER;
        createdAt = currentInstant;
        updatedAt = currentInstant;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public String getHandle() { return handle; }
    public String getDisplayName() { return displayName; }

    public void setRole(UserRole role) { this.role = role; }
}
