package com.example.coffeenotes.api.dto.recipe;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteResponseDTO {
    private UUID trackId;
    private boolean favorite;
}
