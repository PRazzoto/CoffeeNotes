package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class RecipeVersionResponseDTO {
    private UUID trackId;
    private UUID versionId;
    private Integer versionNumber;
    private boolean isCurrent;
    private UUID beanId;
    private UUID methodId;
    private String title;
    private boolean isGlobal;
    private LocalDateTime updatedAt;
}
