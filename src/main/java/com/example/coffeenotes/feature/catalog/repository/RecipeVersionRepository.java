package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.RecipeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeVersionRepository extends JpaRepository<RecipeVersion, UUID> {
    Optional<RecipeVersion>
    findByTrack_IdAndIsCurrentTrue(UUID trackID);

    List<RecipeVersion>
    findByTrack_IdOrderByVersionNumberDesc(UUID trackId);

    Optional<RecipeVersion>
    findTopByTrack_IdOrderByVersionNumberDesc(UUID trackId);
}