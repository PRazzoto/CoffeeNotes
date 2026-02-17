package com.example.coffeenotes.feature.auth.repository;

import com.example.coffeenotes.domain.auth.AuthRefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthRefreshSessionRepository extends JpaRepository<AuthRefreshSession, UUID> {
    Optional<AuthRefreshSession> findByTokenHash(String tokenHash);

    List<AuthRefreshSession> findByUser_IdAndRevokedAtIsNull(UUID userId);

    void deleteByUser_Id(UUID userId);
}