package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class RecipeFilterDTO {
    private UUID methodId;
    private UUID beanId;
    private UUID equipmentId;
    private Boolean isGlobal;
    private Boolean hasBean;
    private Boolean favoritesOnly;
    private Integer ratingMin;
    private Integer ratingMax;
    private Integer brewTimeMinSeconds;
    private Integer brewTimeMaxSeconds;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedTo;
    private String q;
}
