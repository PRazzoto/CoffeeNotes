package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.recipe.FavoriteResponseDTO;
import com.example.coffeenotes.api.dto.recipe.TrackSummaryResponseDTO;
import com.example.coffeenotes.domain.catalog.Favorite;
import com.example.coffeenotes.domain.catalog.FavoriteId;
import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.FavoriteRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeTrackRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sound.midi.Track;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor

public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final RecipeTrackRepository recipeTrackRepository;
    private final UserRepository userRepository;

    public FavoriteResponseDTO addFavorite(UUID userId, UUID trackId) {
        if(userId == null || trackId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields missing.");
        }

        RecipeTrack track = recipeTrackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found."));

        if(track.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found.");
        }
        boolean visible = track.isGlobal() || track.getOwner().getId().equals(userId);
        if(!visible) {
           throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found.");
        }
        if(favoriteRepository.existsByUser_IdAndRecipeTrack_Id(userId, trackId)) {
            FavoriteResponseDTO dto = new FavoriteResponseDTO();
            dto.setFavorite(true);
            dto.setTrackId(trackId);
            return dto;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        Favorite fav = new Favorite();
        fav.setId(new FavoriteId(userId, trackId));
        fav.setUser(user);
        fav.setRecipeTrack(track);
        favoriteRepository.save(fav);

        FavoriteResponseDTO dto = new FavoriteResponseDTO();
        dto.setFavorite(true);
        dto.setTrackId(trackId);
        return dto;
    }

    public FavoriteResponseDTO removeFavorite(UUID userId, UUID trackId) {
        if(userId == null || trackId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields missing.");
        }

        RecipeTrack track = recipeTrackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found."));

        if(track.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found.");
        }
        boolean visible = track.isGlobal() || track.getOwner().getId().equals(userId);
        if(!visible) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found.");
        }

        if(favoriteRepository.existsByUser_IdAndRecipeTrack_Id(userId, trackId)) {
            favoriteRepository.deleteByUser_IdAndRecipeTrack_Id(userId, trackId);
        }
        FavoriteResponseDTO dto = new FavoriteResponseDTO();
        dto.setFavorite(false);
        dto.setTrackId(trackId);
        return dto;
    }

    public List<TrackSummaryResponseDTO> listFavoriteRecipes(UUID userId) {
        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields missing.");
        }
        return favoriteRepository.findByUser_Id(userId).stream()
                .map(Favorite::getRecipeTrack)
                .filter(track -> track.getDeletedAt() == null)
                .filter(track -> track.isGlobal() || track.getOwner().getId().equals(userId))
                .map(track -> {
                    TrackSummaryResponseDTO dto = toTrackSummaryResponseDTO(track);
                    dto.setFavorite(true);
                    return dto;
                })
                .toList();
    }

    private TrackSummaryResponseDTO toTrackSummaryResponseDTO(RecipeTrack track) {
        TrackSummaryResponseDTO dto = new TrackSummaryResponseDTO();
        dto.setTrackId(track.getId());
        dto.setTitle(track.getTitle());
        dto.setGlobal(track.isGlobal());
        dto.setUpdatedAt(track.getUpdatedAt());
        return dto;
    }
}
