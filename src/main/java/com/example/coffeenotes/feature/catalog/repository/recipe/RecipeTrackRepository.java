package com.example.coffeenotes.feature.catalog.repository.recipe;

import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeTrackRepository extends JpaRepository<RecipeTrack, UUID> {
    Optional<RecipeTrack> findByOwner_IdAndBean_IdAndMethod_IdAndDeletedAtIsNull(UUID ownerId, UUID beanId, UUID methodId);

    Optional<RecipeTrack> findByIdAndOwner_IdAndDeletedAtIsNull(UUID trackId, UUID ownerId);

    List<RecipeTrack> findAllByOwner_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID ownerId);
}
