package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.UUID;


public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    @Query(value = """
        select *
        from coffeenotes.recipes r
        where r.deleted_at is null
          and (r.owner_id = :userId or r.is_global = true)
        order by r.created_at desc
        """, nativeQuery = true)
    List<Recipe> findVisibleByUserId(@Param("userId") UUID userId);
}
