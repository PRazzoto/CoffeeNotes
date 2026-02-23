package com.example.coffeenotes.feature.user.repository;

import com.example.coffeenotes.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Modifying
    @Query(value = "delete from coffeenotes.media_assets where owner_id = :userId", nativeQuery = true)
    void deleteMediaAssetsByOwnerId(@Param("userId") UUID userId);
}