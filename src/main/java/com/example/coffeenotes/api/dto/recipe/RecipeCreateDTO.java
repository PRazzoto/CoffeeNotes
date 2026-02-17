package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RecipeCreateDTO {
    private UUID methodId;
    private String title;

    private String coffeeAmount;
    private String waterAmount;
    private String grindSize;
    private Integer brewTimeSeconds;
    private Integer waterTemperatureCelsius;
    private Integer rating;
    private Boolean isGlobal;
}
