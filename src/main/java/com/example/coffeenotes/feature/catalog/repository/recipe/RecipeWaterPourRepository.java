package com.example.coffeenotes.feature.catalog.repository.recipe;

import com.example.coffeenotes.domain.catalog.recipe.RecipeWaterPour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeWaterPourRepository extends JpaRepository<RecipeWaterPour, UUID> {
    List<RecipeWaterPour>
    findByRecipeVersion_IdOrderByOrderIndexAsc(UUID recipeVersionId);

    void deleteByRecipeVersion_Id(UUID recipeVersionId);
}