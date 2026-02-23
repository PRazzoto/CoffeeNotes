package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.recipe.RecipeCreateDTO;
import com.example.coffeenotes.api.dto.recipe.RecipeResponseDTO;
import com.example.coffeenotes.api.dto.recipe.RecipeUpdateDTO;
import com.example.coffeenotes.feature.catalog.service.RecipeService;
import com.example.coffeenotes.util.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipe")
public class RecipeController {
    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/getRecipes")
    public List<RecipeResponseDTO> getRecipes(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        return recipeService.listByUserId(userId);
    }

    @PostMapping("/createRecipe")
    public ResponseEntity<RecipeResponseDTO> createRecipe(@AuthenticationPrincipal Jwt jwt, @RequestBody RecipeCreateDTO body) {
        UUID userId = JwtUtils.extractUserId(jwt);
        RecipeResponseDTO created = recipeService.create(userId, body);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/updateRecipe/{id}")
    public ResponseEntity<RecipeResponseDTO> updateRecipe(@PathVariable UUID id, @RequestBody RecipeUpdateDTO body, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        RecipeResponseDTO updated = recipeService.updateRecipe(id, body, userId);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/deleteRecipe/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        recipeService.delete(id, userId);
    }
}
