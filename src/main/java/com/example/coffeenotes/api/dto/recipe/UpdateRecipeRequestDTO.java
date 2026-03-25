package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateRecipeRequestDTO {
    private String title;
    private String coffeeAmount;
    private String waterAmount;
    private Integer grindSize;
    private Integer brewTimeSeconds;
    private Integer waterTemperatureCelsius;
    private Integer rating;
    private String methodPayload;
    private List<WaterPourDTO> waterPours;
    private List<UUID> equipmentIds;
}
