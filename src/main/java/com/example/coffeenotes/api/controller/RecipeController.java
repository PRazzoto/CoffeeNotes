package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.RecipeCreateDTO;
import com.example.coffeenotes.api.dto.RecipeResponseDTO;
import com.example.coffeenotes.api.dto.RecipeUpdateDTO;
import com.example.coffeenotes.feature.catalog.service.RecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public List<RecipeResponseDTO> getRecipes(@RequestParam UUID userId) {
        return recipeService.listByUserId(userId);
    }

    @PostMapping("/createRecipe")
    public ResponseEntity<RecipeResponseDTO> createRecipe(@RequestParam UUID userId, @RequestBody RecipeCreateDTO body) {
        RecipeResponseDTO created = recipeService.create(userId, body);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/updateRecipe/{id}")
    public ResponseEntity<RecipeResponseDTO> updateRecipe(@PathVariable UUID id, @RequestBody RecipeUpdateDTO body, @RequestParam UUID userId) {
        RecipeResponseDTO updated = recipeService.updateRecipe(id, body, userId);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/deleteRecipe/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam UUID userId) {
        recipeService.delete(id, userId);
    }

}
