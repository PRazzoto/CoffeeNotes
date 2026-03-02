package com.example.coffeenotes.feature.user.service;

import com.example.coffeenotes.api.dto.user.UpdatePasswordDTO;
import com.example.coffeenotes.api.dto.user.UpdateRequestDTO;
import com.example.coffeenotes.api.dto.user.UserReturnDTO;
import com.example.coffeenotes.domain.auth.AuthRefreshSession;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import com.example.coffeenotes.domain.catalog.recipe.RecipeVersion;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.auth.repository.AuthRefreshSessionRepository;
import com.example.coffeenotes.feature.catalog.repository.CoffeeBeanRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeEquipmentRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeTrackRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeVersionRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeWaterPourRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import com.example.coffeenotes.util.PasswordValidator;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final RecipeTrackRepository recipeTrackRepository;
    private final RecipeVersionRepository recipeVersionRepository;
    private final RecipeWaterPourRepository recipeWaterPourRepository;
    private final RecipeEquipmentRepository recipeEquipmentRepository;
    private final CoffeeBeanRepository coffeeBeanRepository;

    public UserReturnDTO getUser(UUID userId) {
        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Id.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserReturnDTO response = new UserReturnDTO();
        response.setDisplayName(user.getDisplayName());
        response.setEmail(user.getEmail());

        return response;
    }

    public UpdateRequestDTO updateUser(UUID userId, UpdateRequestDTO dto) {

        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Id.");
        }
        if(dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body not valid.");
        }

        User currentUser= userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if(dto.getDisplayName() == null || dto.getDisplayName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be empty.");
        }
        String displayName = dto.getDisplayName().trim();
        String currentName = currentUser.getDisplayName();

        if(currentName != null && displayName.equals(currentName.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New name must be different.");
        }

        currentUser.setDisplayName(displayName);
        userRepository.save(currentUser);

        UpdateRequestDTO dtoNew = new UpdateRequestDTO();
        dtoNew.setDisplayName(displayName);

        return dtoNew;
    }

    @Transactional
    public void updatePassword(UpdatePasswordDTO dto, UUID userId) {
        if(dto == null || dto.getCurrentPassword() == null || dto.getNewPassword() == null || userId == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fields must not be null");
        }

        if(dto.getCurrentPassword().isBlank()|| dto.getNewPassword().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fields must not be blank.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));


        String currentPassword = dto.getCurrentPassword();
        String newPassword = dto.getNewPassword();
        if(!passwordEncoder.matches(currentPassword, user.getPasswordHash())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        if(passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password should be different.");
        }

        if(!PasswordValidator.isValid(newPassword)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password does not meet the requirements.");
        }

        String hashedPassword = passwordEncoder.encode(newPassword);

        user.setPasswordHash(hashedPassword);

        userRepository.save(user);

        List<AuthRefreshSession> activeSessions = authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();

        for(AuthRefreshSession session : activeSessions) {
           session.setRevokedAt(now);
        }
        authRefreshSessionRepository.saveAll(activeSessions);
    }
     @Transactional
     public void deleteUser(UUID userId) {
        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Id.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        List<RecipeTrack> ownedTracks = recipeTrackRepository.findAllByOwner_Id(userId);
        List<UUID> trackIds = ownedTracks.stream().map(RecipeTrack::getId).toList();

        if (!trackIds.isEmpty()) {
            List<RecipeVersion> allVersions = recipeVersionRepository.findByTrack_IdIn(trackIds);
            List<UUID> versionIds = allVersions.stream().map(RecipeVersion::getId).toList();
            if (!versionIds.isEmpty()) {
                recipeWaterPourRepository.deleteByRecipeVersion_IdIn(versionIds);
                recipeEquipmentRepository.deleteByRecipeVersion_IdIn(versionIds);
            }
            recipeVersionRepository.deleteAll(allVersions);
        }

        recipeTrackRepository.deleteAll(ownedTracks);
        List<CoffeeBean> ownedBeans = coffeeBeanRepository.findAllByOwner_Id(userId);
        coffeeBeanRepository.deleteAll(ownedBeans);

        authRefreshSessionRepository.deleteByUser_Id(userId);
        userRepository.deleteMediaAssetsByOwnerId(userId);
        userRepository.delete(user);
     }
}

