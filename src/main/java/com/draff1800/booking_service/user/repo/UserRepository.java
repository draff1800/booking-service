package com.draff1800.booking_service.user.repo;

import com.draff1800.booking_service.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByIdIn(Collection<UUID> ids);
    boolean existsByHandle(String handle);
}
