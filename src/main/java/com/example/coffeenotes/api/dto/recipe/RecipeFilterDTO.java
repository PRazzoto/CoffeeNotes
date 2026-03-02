package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RecipeFilterDTO {
    private UUID methodId;
    private Boolean isGlobal;
    private Boolean favoritesOnly;
}
