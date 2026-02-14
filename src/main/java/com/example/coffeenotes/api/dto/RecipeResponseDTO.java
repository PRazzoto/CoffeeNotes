package com.example.coffeenotes.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class RecipeResponseDTO {
    private UUID id;
    private UUID ownerId;
    private UUID methodId;
    private String title;
    private String coffeeAmount;
    private String waterAmount;
    private String grindSize;
    private Integer brewTimeSeconds;
    private Integer waterTemperatureCelsius;
    private Integer rating;
    private Boolean isGlobal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
