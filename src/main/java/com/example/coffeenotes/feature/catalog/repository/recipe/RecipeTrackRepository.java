package com.example.coffeenotes.feature.catalog.repository.recipe;

import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeTrackRepository extends JpaRepository<RecipeTrack, UUID> {
    Optional<RecipeTrack> findByOwner_IdAndBean_IdAndMethod_IdAndDeletedAtIsNull(UUID ownerId, UUID beanId, UUID methodId);

    Optional<RecipeTrack> findByIdAndOwner_IdAndDeletedAtIsNull(UUID trackId, UUID ownerId);

    List<RecipeTrack> findAllByOwner_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID ownerId);

    List<RecipeTrack> findAllByOwner_Id(UUID ownerId);

    @Query(
            value = "SELECT DISTINCT t FROM RecipeTrack t " +
                    "JOIN RecipeVersion v ON v.track = t AND v.isCurrent = true AND v.deletedAt IS NULL " +
                    "JOIN t.method m " +
                    "LEFT JOIN t.bean b " +
                    "WHERE t.deletedAt IS NULL " +
                    "AND (t.owner.id = :ownerId OR t.isGlobal = true) " +
                    "AND (:methodId IS NULL OR m.id = :methodId) " +
                    "AND (:beanId IS NULL OR (b IS NOT NULL AND b.id = :beanId)) " +
                    "AND (:equipmentId IS NULL OR EXISTS (" +
                    "       SELECT 1 FROM RecipeEquipment re " +
                    "       WHERE re.recipeVersion = v AND re.equipment.id = :equipmentId" +
                    "    )) " +
                    "AND (:isGlobal IS NULL OR t.isGlobal = :isGlobal) " +
                    "AND (:hasBean IS NULL OR (:hasBean = true AND t.bean IS NOT NULL) OR (:hasBean = false AND t.bean IS NULL)) " +
                    "AND (:favoriteOnly = FALSE OR EXISTS (" +
                    "       SELECT 1 FROM Favorite f WHERE f.recipeTrack = t AND f.user.id = :ownerId" +
                    "    )) " +
                    "AND (:ratingMin IS NULL OR v.rating >= :ratingMin) " +
                    "AND (:ratingMax IS NULL OR v.rating <= :ratingMax) " +
                    "AND (:brewTimeMinSeconds IS NULL OR v.brewTimeSeconds >= :brewTimeMinSeconds) " +
                    "AND (:brewTimeMaxSeconds IS NULL OR v.brewTimeSeconds <= :brewTimeMaxSeconds) " +
                    "AND (:applyUpdatedFrom = false OR v.updatedAt >= :updatedFrom) " +
                    "AND (:applyUpdatedTo = false OR v.updatedAt <= :updatedTo) " +
                    "AND (:qEnabled = false OR " +
                    "     lower(t.title) LIKE :qPattern OR " +
                    "     lower(m.name) LIKE :qPattern OR " +
                    "     (b IS NOT NULL AND lower(b.name) LIKE :qPattern) " +
                    ") " +
                    "ORDER BY t.updatedAt DESC",
            countQuery = "SELECT COUNT(DISTINCT t) FROM RecipeTrack t " +
                    "JOIN RecipeVersion v ON v.track = t AND v.isCurrent = true AND v.deletedAt IS NULL " +
                    "JOIN t.method m " +
                    "LEFT JOIN t.bean b " +
                    "WHERE t.deletedAt IS NULL " +
                    "AND (t.owner.id = :ownerId OR t.isGlobal = true) " +
                    "AND (:methodId IS NULL OR m.id = :methodId) " +
                    "AND (:beanId IS NULL OR (b IS NOT NULL AND b.id = :beanId)) " +
                    "AND (:equipmentId IS NULL OR EXISTS (" +
                    "       SELECT 1 FROM RecipeEquipment re " +
                    "       WHERE re.recipeVersion = v AND re.equipment.id = :equipmentId" +
                    "    )) " +
                    "AND (:isGlobal IS NULL OR t.isGlobal = :isGlobal) " +
                    "AND (:hasBean IS NULL OR (:hasBean = true AND t.bean IS NOT NULL) OR (:hasBean = false AND t.bean IS NULL)) " +
                    "AND (:favoriteOnly = FALSE OR EXISTS (" +
                    "       SELECT 1 FROM Favorite f WHERE f.recipeTrack = t AND f.user.id = :ownerId" +
                    "    )) " +
                    "AND (:ratingMin IS NULL OR v.rating >= :ratingMin) " +
                    "AND (:ratingMax IS NULL OR v.rating <= :ratingMax) " +
                    "AND (:brewTimeMinSeconds IS NULL OR v.brewTimeSeconds >= :brewTimeMinSeconds) " +
                    "AND (:brewTimeMaxSeconds IS NULL OR v.brewTimeSeconds <= :brewTimeMaxSeconds) " +
                    "AND (:applyUpdatedFrom = false OR v.updatedAt >= :updatedFrom) " +
                    "AND (:applyUpdatedTo = false OR v.updatedAt <= :updatedTo) " +
                    "AND (:qEnabled = false OR " +
                    "     lower(t.title) LIKE :qPattern OR " +
                    "     lower(m.name) LIKE :qPattern OR " +
                    "     (b IS NOT NULL AND lower(b.name) LIKE :qPattern) " +
                    ")"
    )
    Page<RecipeTrack> findVisibleTracks(
            @Param("ownerId") UUID ownerId,
            @Param("methodId") UUID methodId,
            @Param("beanId") UUID beanId,
            @Param("equipmentId") UUID equipmentId,
            @Param("isGlobal") Boolean isGlobal,
            @Param("hasBean") Boolean hasBean,
            @Param("favoriteOnly") boolean favoriteOnly,
            @Param("ratingMin") Integer ratingMin,
            @Param("ratingMax") Integer ratingMax,
            @Param("brewTimeMinSeconds") Integer brewTimeMinSeconds,
            @Param("brewTimeMaxSeconds") Integer brewTimeMaxSeconds,
            @Param("applyUpdatedFrom") boolean applyUpdatedFrom,
            @Param("updatedFrom") LocalDateTime updatedFrom,
            @Param("applyUpdatedTo") boolean applyUpdatedTo,
            @Param("updatedTo") LocalDateTime updatedTo,
            @Param("qEnabled") boolean qEnabled,
            @Param("qPattern") String qPattern,
            Pageable pageable);
}
