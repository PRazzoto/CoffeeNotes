package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TrackDetailsResponseDTO {
    private UUID trackId;
    private UUID beanId;
    private String beanName;
    private String roaster;
    private String origin;
    private String process;
    private String notes;
    private UUID methodId;
    private String methodName;
    private String title;
    private boolean isGlobal;
    private boolean favorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID versionId;
    private Integer versionNumber;
    private boolean isCurrent;
    private String coffeeAmount;
    private String waterAmount;
    private String grindSize;
    private Integer brewTimeSeconds;
    private Integer waterTemperatureCelsius;
    private Integer rating;
    private String methodPayload;
    private LocalDateTime versionUpdatedAt;
    private List<WaterPourDTO> waterPours;
    private List<UUID> equipmentIds;
}
