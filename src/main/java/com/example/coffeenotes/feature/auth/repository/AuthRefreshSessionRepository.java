package com.example.coffeenotes.feature.auth.repository;

import com.example.coffeenotes.domain.auth.AuthRefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthRefreshSessionRepository extends JpaRepository<AuthRefreshSession, UUID> {
    Optional<AuthRefreshSession> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthRefreshSession s WHERE s.tokenHash = :tokenHash")
    Optional<AuthRefreshSession> findByTokenHashWithLock(@Param("tokenHash") String tokenHash);

    List<AuthRefreshSession> findByUser_IdAndRevokedAtIsNull(UUID userId);

    void deleteByUser_Id(UUID userId);
}