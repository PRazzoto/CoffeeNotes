package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.Favorite;
import com.example.coffeenotes.domain.catalog.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    List<Favorite> findByUser_Id(UUID userId);

    boolean existsByUser_IdAndRecipeTrack_Id(UUID userId, UUID recipeTrackId);

    void deleteByUser_IdAndRecipeTrack_Id(UUID userId, UUID recipeTrackId);
}