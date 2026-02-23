package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.UUID;


public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    @Query("""
        select r
        from Recipe r
        where r.deletedAt is null
          and (r.owner.id = :userId or r.isGlobal = true)
        order by r.createdAt desc
        """)
    List<Recipe> findVisibleByUserId(@Param("userId") UUID userId);
    @Modifying
     void deleteByOwner_Id(UUID ownerId);
}
