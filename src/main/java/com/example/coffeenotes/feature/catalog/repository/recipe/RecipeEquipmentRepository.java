package com.example.coffeenotes.feature.catalog.repository.recipe;

import com.example.coffeenotes.domain.catalog.recipe.RecipeEquipment;
import com.example.coffeenotes.domain.catalog.recipe.RecipeEquipmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeEquipmentRepository extends JpaRepository<RecipeEquipment, RecipeEquipmentId> {
    List<RecipeEquipment> findByRecipeVersion_Id(UUID recipeVersionId);

    void deleteByRecipeVersion_Id(UUID recipeVersionId);
}