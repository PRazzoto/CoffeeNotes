package com.example.coffeenotes.feature.catalog.repository.recipe;

import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeTrackRepository extends JpaRepository<RecipeTrack, UUID> {
    Optional<RecipeTrack> findByOwner_IdAndBean_IdAndMethod_IdAndDeletedAtIsNull(UUID ownerId, UUID beanId, UUID methodId);

    Optional<RecipeTrack> findByIdAndOwner_IdAndDeletedAtIsNull(UUID trackId, UUID ownerId);

    List<RecipeTrack> findAllByOwner_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID ownerId);

    List<RecipeTrack> findAllByOwner_Id(UUID ownerId);

    @Query("SELECT t FROM RecipeTrack t WHERE t.deletedAt IS NULL " +
           "AND (t.owner.id = :ownerId OR t.global = true) " +
           "AND (:methodId IS NULL OR t.method.id = :methodId) " +
           "AND (:isGlobal IS NULL OR t.global = :isGlobal) " +
           "AND (:favoriteOnly = FALSE OR " +
           "     EXISTS (SELECT f FROM Favorite f WHERE f.recipeTrack = t AND f.user.id = :ownerId)) " +
           "ORDER BY t.updatedAt DESC")
    Page<RecipeTrack> findVisibleTracks(
            @Param("ownerId") UUID ownerId,
            @Param("methodId") UUID methodId,
            @Param("isGlobal") Boolean isGlobal,
            @Param("favoriteOnly") boolean favoriteOnly,
            Pageable pageable);
}
