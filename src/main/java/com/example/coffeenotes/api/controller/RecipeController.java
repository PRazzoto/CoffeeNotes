package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.common.PagedResponseDTO;
import com.example.coffeenotes.api.dto.recipe.*;
import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.example.coffeenotes.feature.catalog.service.FavoriteService;
import com.example.coffeenotes.feature.catalog.service.RecipeVersionService;
import com.example.coffeenotes.util.JwtUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final RecipeVersionService recipeService;
    private final FavoriteService favoriteService;

    public RecipeController(RecipeVersionService recipeService, FavoriteService favoriteService) {
        this.recipeService = recipeService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/getRecipes")
    public PagedResponseDTO<TrackSummaryResponseDTO> getRecipes(@AuthenticationPrincipal Jwt jwt, RecipeFilterDTO filter, Pageable pageable) {
        UUID userId = JwtUtils.extractUserId(jwt);
        Page<TrackSummaryResponseDTO> page = recipeService.listRecipes(userId, filter, pageable);
        return PagedResponseDTO.from(page);
    }

    @PostMapping("/createRecipe")
    public ResponseEntity<RecipeVersionResponseDTO> createRecipe(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateTrackRequestDTO body) {
        UUID userId = JwtUtils.extractUserId(jwt);
        RecipeVersionResponseDTO created = recipeService.createRecipe(userId, body);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/updateRecipe/{trackId}")
    public ResponseEntity<RecipeVersionResponseDTO> updateRecipe(@PathVariable UUID trackId, @RequestBody UpdateRecipeRequestDTO body, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        RecipeVersionResponseDTO updated = recipeService.updateRecipe(userId, trackId, body);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/deleteRecipe/{trackId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID trackId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        recipeService.deleteRecipe(userId, trackId);
    }

    @GetMapping("/getRecipe/{trackId}")
    public TrackDetailsResponseDTO getRecipe(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID trackId) {
        UUID userId = JwtUtils.extractUserId(jwt);
        return recipeService.getRecipe(userId, trackId);
    }

    @GetMapping("/getRecipeVersions/{trackId}")
    public List<VersionHistoryItemDTO> getRecipeVersions(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID trackId) {
        UUID userId = JwtUtils.extractUserId(jwt);
        return recipeService.listRecipeVersions(userId, trackId);
    }

    @GetMapping("/methods/{methodId}/metadata")
    public MethodPayloadMetadataDTO getMetadata(@PathVariable UUID methodId) {
        return recipeService.getMetadata(methodId);
    }

    @PostMapping("/{trackId}/favorite")
    public ResponseEntity<FavoriteResponseDTO> addFavorite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID trackId) {
        UUID userId = JwtUtils.extractUserId(jwt);
        FavoriteResponseDTO dto = favoriteService.addFavorite(userId, trackId);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @DeleteMapping("/{trackId}/favorite")
    public ResponseEntity<FavoriteResponseDTO> deleteFavorite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID trackId) {
        UUID userId = JwtUtils.extractUserId(jwt);
        FavoriteResponseDTO dto = favoriteService.removeFavorite(userId, trackId);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/favorites")
    public List<TrackSummaryResponseDTO> listFavorites(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        return favoriteService.listFavoriteRecipes(userId);
    }
}
