package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}