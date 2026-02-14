package com.example.coffeenotes.feature.catalog.service;


import com.example.coffeenotes.api.dto.RecipeCreateDTO;
import com.example.coffeenotes.api.dto.RecipeResponseDTO;
import com.example.coffeenotes.api.dto.RecipeUpdateDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.domain.catalog.Recipe;
import com.example.coffeenotes.domain.catalog.User;
import com.example.coffeenotes.feature.catalog.repository.BrewMethodsRepository;
import com.example.coffeenotes.feature.catalog.repository.RecipeRepository;
import com.example.coffeenotes.feature.catalog.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final BrewMethodsRepository brewMethodsRepository;
    private final UserRepository userRepository;

    public RecipeResponseDTO create(UUID userId, RecipeCreateDTO body){
        if(body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required");
        }
        if(userId == null||body.getMethodId() == null ||body.getTitle() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is a required field that is missing");
        }
        if(body.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title should not be blank");
        }
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        BrewMethods method = brewMethodsRepository.findById(body.getMethodId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Method not found."));

        if(body.getRating() != null){
            if(body.getRating() > 5 || body.getRating() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating should be between 1 and 5");
            }
        }

        if(body.getBrewTimeSeconds() != null && body.getBrewTimeSeconds() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brew time should not be negative");
        }

        if(body.getWaterTemperatureCelsius() != null && body.getWaterTemperatureCelsius() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Temperature of the water should not be below 0");
        }

        Recipe recipe = new Recipe();
        recipe.setOwner(owner);
        recipe.setMethod(method);
        recipe.setTitle(body.getTitle());
        recipe.setCoffeeAmount(body.getCoffeeAmount());
        recipe.setWaterAmount(body.getWaterAmount());
        recipe.setGrindSize(body.getGrindSize());
        recipe.setBrewTimeSeconds(body.getBrewTimeSeconds());
        recipe.setWaterTemperatureCelsius(body.getWaterTemperatureCelsius());
        recipe.setRating(body.getRating());
        recipe.setGlobal(Boolean.TRUE.equals(body.getIsGlobal()));

        return toResponseDTO(recipeRepository.save(recipe));
    }

    public List<RecipeResponseDTO> listByUserId(UUID userId) {
        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required.");
        }
       return recipeRepository.findVisibleByUserId(userId)
               .stream()
               .map(this::toResponseDTO)
               .toList();
    }

    public RecipeResponseDTO updateRecipe(UUID id, RecipeUpdateDTO body, UUID userId) {
        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        Recipe existing = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found."));
        if(existing.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found.");
        }
        if(!existing.getOwner().getId().equals(userId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found.");
        }

        if(body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        if(body.getMethodId() == null && body.getTitle() == null && body.getCoffeeAmount() == null 
                && body.getWaterAmount() == null && body.getGrindSize() == null 
                && body.getBrewTimeSeconds() == null && body.getWaterTemperatureCelsius() == null 
                && body.getRating() == null && body.getIsGlobal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field is required for update");
        }

        if(body.getMethodId() != null) {
            BrewMethods method = brewMethodsRepository.findById(body.getMethodId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brew Method not found"));
            existing.setMethod(method);
        }

        if(body.getTitle() != null) {
            if(body.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must not be blank");
            }
            existing.setTitle(body.getTitle());
        }

        if(body.getCoffeeAmount() != null) existing.setCoffeeAmount(body.getCoffeeAmount());
        if(body.getWaterAmount() != null) existing.setWaterAmount(body.getWaterAmount());
        if(body.getGrindSize() != null) existing.setGrindSize(body.getGrindSize());

        if(body.getBrewTimeSeconds() != null) {
            if(body.getBrewTimeSeconds() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brew time should not be negative");
            }
            existing.setBrewTimeSeconds(body.getBrewTimeSeconds());
        }

        if(body.getWaterTemperatureCelsius() != null) {
            if(body.getWaterTemperatureCelsius() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Temperature of the water should not be below 0");
            }
            existing.setWaterTemperatureCelsius(body.getWaterTemperatureCelsius());
        }

        if(body.getRating() != null){
            if(body.getRating() > 5 || body.getRating() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating should be between 1 and 5");
            }
            existing.setRating(body.getRating());
        }

        if(body.getIsGlobal() != null) {
            existing.setGlobal(body.getIsGlobal());
        }

        Recipe updated = recipeRepository.save(existing);
        return toResponseDTO(updated);

    }


    public void delete(UUID id, UUID userId){
        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        if(recipe.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found.");
        }
        if(!recipe.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found.");
        }
        recipe.setDeletedAt(LocalDateTime.now());

        recipeRepository.save(recipe);

    }

    private RecipeResponseDTO toResponseDTO(Recipe recipe) {
        RecipeResponseDTO dto = new RecipeResponseDTO();
        dto.setId(recipe.getId());
        dto.setOwnerId(recipe.getOwner().getId());
        dto.setMethodId(recipe.getMethod().getId());
        dto.setTitle(recipe.getTitle());
        dto.setCoffeeAmount(recipe.getCoffeeAmount());
        dto.setWaterAmount(recipe.getWaterAmount());
        dto.setGrindSize(recipe.getGrindSize());
        dto.setBrewTimeSeconds(recipe.getBrewTimeSeconds());
        dto.setWaterTemperatureCelsius(recipe.getWaterTemperatureCelsius());
        dto.setRating(recipe.getRating());
        dto.setIsGlobal(recipe.isGlobal());
        dto.setCreatedAt(recipe.getCreatedAt());
        dto.setUpdatedAt(recipe.getUpdatedAt());
        return dto;
    }

}
