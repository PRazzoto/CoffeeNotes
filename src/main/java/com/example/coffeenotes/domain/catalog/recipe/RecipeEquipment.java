package com.example.coffeenotes.domain.catalog.recipe;

import com.example.coffeenotes.domain.catalog.Equipment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recipe_equipment", schema = "coffeenotes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecipeEquipment {
    @EmbeddedId
    private RecipeEquipmentId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("recipeVersionId")
    @JoinColumn(name = "recipe_version_id", nullable = false)
    private RecipeVersion recipeVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("equipmentId")
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;
}
