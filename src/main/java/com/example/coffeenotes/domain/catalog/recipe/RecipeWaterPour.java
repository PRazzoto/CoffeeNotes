package com.example.coffeenotes.domain.catalog.recipe;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "recipe_water_pours", schema = "coffeenotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipeWaterPour {
    private @Id
    @GeneratedValue UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_version_id", nullable = false)
    private RecipeVersion recipeVersion;

    @Column(name = "water_amount_ml", nullable = false)
    private Integer waterAmount;

    @Column(nullable = false, length = 20)
    private String time;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
