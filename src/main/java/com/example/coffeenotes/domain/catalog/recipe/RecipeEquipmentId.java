package com.example.coffeenotes.domain.catalog.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecipeEquipmentId implements Serializable {
    @Column(name = "recipe_version_id", nullable = false)
    private UUID recipeVersionId;

    @Column(name = "equipment_id", nullable = false)
    private UUID equipmentId;
}
