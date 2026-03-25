package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.recipe.*;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.domain.catalog.recipe.*;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.methodpayload.MethodPayloadStrategy;
import com.example.coffeenotes.feature.catalog.methodpayload.MethodPayloadStrategyRegistry;
import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.example.coffeenotes.feature.catalog.repository.BrewMethodsRepository;
import com.example.coffeenotes.feature.catalog.repository.CoffeeBeanRepository;
import com.example.coffeenotes.feature.catalog.repository.EquipmentRepository;
import com.example.coffeenotes.feature.catalog.repository.FavoriteRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeEquipmentRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeTrackRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeVersionRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeWaterPourRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RecipeVersionService {

    private final CoffeeBeanRepository coffeeBeanRepository;
    private final BrewMethodsRepository brewMethodsRepository;
    private final RecipeTrackRepository recipeTrackRepository;
    private final UserRepository userRepository;
    private final RecipeVersionRepository recipeVersionRepository;
    private final FavoriteRepository favoriteRepository;
    private final RecipeWaterPourRepository recipeWaterPourRepository;
    private final RecipeEquipmentRepository recipeEquipmentRepository;
    private final EquipmentRepository equipmentRepository;
    private final MethodPayloadStrategyRegistry methodPayloadStrategyRegistry;
    private final ObjectMapper objectMapper;

    @Transactional
    public RecipeVersionResponseDTO createRecipe(UUID userId, CreateTrackRequestDTO dto) {
        if(dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required");
        }
        if(userId == null || dto.getMethodId() == null || dto.getTitle() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is a required field that is missing");
        }
        String title = dto.getTitle().trim();
        if(title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title should not be blank");
        }
        if (dto.getEquipmentIds() != null) {
            Set<UUID> seenEquipmentIds = new HashSet<>();
            for (UUID equipmentId : dto.getEquipmentIds()) {
                if (equipmentId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Equipment id must not be null.");
                }
                if (!seenEquipmentIds.add(equipmentId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate equipment id.");
                }
            }
        }
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        CoffeeBean bean = null;
        if (dto.getBeanId() != null) {
            bean = coffeeBeanRepository.findById(dto.getBeanId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coffee bean not found"));

            if(bean.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Coffee bean not found");
            }
            if(!bean.getOwner().getId().equals(userId) && !bean.isGlobal()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Coffee bean not found");
            }
        }

        BrewMethods method =  brewMethodsRepository.findById(dto.getMethodId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Method not found"));

        MethodPayloadStrategy strategy = methodPayloadStrategyRegistry.getRequired(method.getName());
        String rawMethodPayload = dto.getMethodPayload();
        if(rawMethodPayload == null || rawMethodPayload.isBlank()) {
            rawMethodPayload = "{}";
        }

        JsonNode payloadJson;
        try {
            payloadJson = objectMapper.readTree(rawMethodPayload);
        } catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid methodPayload JSON.");
        }
        JsonNode normalizedPayload = strategy.validateAndNormalize(payloadJson);
        String normalizedPayloadString;
        try {
            normalizedPayloadString = objectMapper.writeValueAsString(normalizedPayload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize methodPayload JSON.", e);
        }
        if (bean != null) {
            boolean trackAlreadyExists = recipeTrackRepository
                    .findByOwner_IdAndBean_IdAndMethod_IdAndDeletedAtIsNull(userId, bean.getId(), method.getId())
                    .isPresent();

            if(trackAlreadyExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Track already exists for this bean and method");
            }
        }

        RecipeTrack track = new RecipeTrack();
        track.setOwner(owner);
        track.setBean(bean);
        track.setMethod(method);
        track.setTitle(title);
        track.setGlobal(dto.isGlobal());

        RecipeTrack saved = recipeTrackRepository.save(track);

        RecipeVersion version = new RecipeVersion();
        version.setTrack(saved);
        version.setVersionNumber(1);
        version.setCurrent(true);
        version.setTitle(title);
        version.setMethodPayload(normalizedPayloadString);

        RecipeVersion savedVersion = recipeVersionRepository.save(version);
        if (dto.getEquipmentIds() != null) {
            List<UUID> equipmentIds = dto.getEquipmentIds();
            Set<UUID> seen = new HashSet<>();
            for (UUID eid : equipmentIds) {
                if (!seen.add(eid)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate equipment id: " + eid);
                }
            }
            Map<UUID, Equipment> equipmentMap = equipmentRepository.findAllById(equipmentIds).stream()
                    .collect(Collectors.toMap(Equipment::getId, e -> e));
            if (equipmentMap.size() != equipmentIds.size()) {
                Set<UUID> missing = new LinkedHashSet<>(equipmentIds);
                missing.removeAll(equipmentMap.keySet());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found: " + missing);
            }
            List<RecipeEquipment> initialEquipments = equipmentIds.stream().map(eid -> {
                Equipment equipment = equipmentMap.get(eid);
                RecipeEquipment row = new RecipeEquipment();
                row.setId(new RecipeEquipmentId(savedVersion.getId(), equipment.getId()));
                row.setRecipeVersion(savedVersion);
                row.setEquipment(equipment);
                return row;
            }).toList();
            recipeEquipmentRepository.saveAll(initialEquipments);
        }

        RecipeVersionResponseDTO answer = new RecipeVersionResponseDTO();
        answer.setTrackId(saved.getId());
        answer.setVersionId(savedVersion.getId());
        answer.setVersionNumber(savedVersion.getVersionNumber());
        answer.setBeanId(bean != null ? bean.getId() : null);
        answer.setMethodId(method.getId());
        answer.setTitle(saved.getTitle());
        answer.setGlobal(saved.isGlobal());
        answer.setUpdatedAt(savedVersion.getUpdatedAt());

        return answer;
    }

    @Transactional(readOnly = true)
    public Page<TrackSummaryResponseDTO> listRecipes(UUID userId, RecipeFilterDTO filter, Pageable pageable) {
        if(userId == null || pageable == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and pageable parameters must not be null.");
        }

        UUID methodId = filter != null ? filter.getMethodId() : null;
        UUID beanId = filter != null ? filter.getBeanId() : null;
        UUID equipmentId = filter != null ? filter.getEquipmentId() : null;
        Boolean isGlobal = filter != null ? filter.getIsGlobal() : null;
        Boolean hasBean = filter != null ? filter.getHasBean() : null;
        boolean favoriteOnly = filter != null && Boolean.TRUE.equals(filter.getFavoritesOnly());
        Integer ratingMin = filter != null ? filter.getRatingMin() : null;
        Integer ratingMax = filter != null ? filter.getRatingMax() : null;
        Integer brewTimeMinSeconds = filter != null ? filter.getBrewTimeMinSeconds() : null;
        Integer brewTimeMaxSeconds = filter != null ? filter.getBrewTimeMaxSeconds() : null;
        LocalDateTime updatedFrom = filter != null ? filter.getUpdatedFrom() : null;
        LocalDateTime updatedTo = filter != null ? filter.getUpdatedTo() : null;
        String q = filter != null ? filter.getQ() : null;
        if (q != null) {
            q = q.trim();
            if (q.isEmpty()) {
                q = null;
            }
        }
        boolean qEnabled = q != null;
        String qPattern = qEnabled ? "%" + q.toLowerCase(Locale.ROOT) + "%" : "%";

        if (ratingMin != null && (ratingMin < 1 || ratingMin > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ratingMin must be between 1 and 5.");
        }
        if (ratingMax != null && (ratingMax < 1 || ratingMax > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ratingMax must be between 1 and 5.");
        }
        if (ratingMin != null && ratingMax != null && ratingMin > ratingMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ratingMin must be less than or equal to ratingMax.");
        }
        if (brewTimeMinSeconds != null && brewTimeMinSeconds < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "brewTimeMinSeconds must be non-negative.");
        }
        if (brewTimeMaxSeconds != null && brewTimeMaxSeconds < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "brewTimeMaxSeconds must be non-negative.");
        }
        if (brewTimeMinSeconds != null && brewTimeMaxSeconds != null && brewTimeMinSeconds > brewTimeMaxSeconds) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "brewTimeMinSeconds must be less than or equal to brewTimeMaxSeconds.");
        }
        if (updatedFrom != null && updatedTo != null && updatedFrom.isAfter(updatedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "updatedFrom must be before or equal to updatedTo.");
        }
        boolean applyUpdatedFrom = updatedFrom != null;
        boolean applyUpdatedTo = updatedTo != null;
        LocalDateTime effectiveUpdatedFrom = applyUpdatedFrom ? updatedFrom : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime effectiveUpdatedTo = applyUpdatedTo ? updatedTo : LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        Set<UUID> favoriteTracks = favoriteRepository.findByUser_Id(userId).stream()
                .map(f -> f.getRecipeTrack().getId())
                .collect(Collectors.toSet());

        Page<RecipeTrack> trackPage = recipeTrackRepository.findVisibleTracks(
                userId,
                methodId,
                beanId,
                equipmentId,
                isGlobal,
                hasBean,
                favoriteOnly,
                ratingMin,
                ratingMax,
                brewTimeMinSeconds,
                brewTimeMaxSeconds,
                applyUpdatedFrom,
                effectiveUpdatedFrom,
                applyUpdatedTo,
                effectiveUpdatedTo,
                qEnabled,
                qPattern,
                pageable
        );

        List<UUID> trackIds = trackPage.getContent().stream().map(RecipeTrack::getId).toList();
        Map<UUID, RecipeVersion> currentVersionsByTrackId = trackIds.isEmpty()
                ? Map.of()
                : recipeVersionRepository.findByTrack_IdInAndIsCurrentTrue(trackIds).stream()
                        .collect(Collectors.toMap(
                                v -> v.getTrack().getId(),
                                v -> v,
                                (left, right) -> {
                                    Integer leftVersion = left.getVersionNumber();
                                    Integer rightVersion = right.getVersionNumber();
                                    if (leftVersion == null) {
                                        return right;
                                    }
                                    if (rightVersion == null) {
                                        return left;
                                    }
                                    return rightVersion >= leftVersion ? right : left;
                                }
                        ));

        List<TrackSummaryResponseDTO> summaries = trackPage.getContent().stream().map(track -> {
            TrackSummaryResponseDTO dto = new TrackSummaryResponseDTO();
            dto.setTrackId(track.getId());
            CoffeeBean bean = track.getBean();
            dto.setBeanId(bean != null ? bean.getId() : null);
            dto.setBeanName(bean != null ? bean.getName() : null);
            dto.setMethodId(track.getMethod().getId());
            dto.setMethodName(track.getMethod().getName());
            dto.setTitle(track.getTitle());
            dto.setGlobal(track.isGlobal());
            dto.setFavorite(favoriteTracks.contains(track.getId()));

            RecipeVersion v = currentVersionsByTrackId.get(track.getId());
            if (v != null) {
                dto.setCurrentVersionNumber(v.getVersionNumber());
                dto.setRating(v.getRating());
                dto.setUpdatedAt(v.getUpdatedAt());
            }

            return dto;
        }).toList();

        return new PageImpl<>(summaries, pageable, trackPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TrackDetailsResponseDTO getRecipe(UUID userId, UUID trackId) {
        if(userId == null || trackId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId or trackId null.");
        }
        RecipeTrack recipe = recipeTrackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found."));
        if(recipe.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found.");
        }
        if(recipe.getOwner().getId().equals(userId) || recipe.isGlobal()) {
            RecipeVersion version = recipeVersionRepository.findByTrack_IdAndIsCurrentTrue(trackId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
            List<RecipeWaterPour> waterPours = recipeWaterPourRepository.findByRecipeVersion_IdOrderByOrderIndexAsc(version.getId());
            List<RecipeEquipment> recipeEquipments = recipeEquipmentRepository.findByRecipeVersion_Id(version.getId());
            boolean exists = favoriteRepository.existsByUser_IdAndRecipeTrack_Id(userId, trackId);
            TrackDetailsResponseDTO dto = new TrackDetailsResponseDTO();

            dto.setTrackId(recipe.getId());
            CoffeeBean bean = recipe.getBean();
            dto.setBeanId(bean != null ? bean.getId() : null);
            dto.setBeanName(bean != null ? bean.getName() : null);
            dto.setRoaster(bean != null ? bean.getRoaster() : null);
            dto.setOrigin(bean != null ? bean.getOrigin() : null);
            dto.setProcess(bean != null ? bean.getProcess() : null);
            dto.setNotes(bean != null ? bean.getNotes() : null);

            dto.setMethodId(recipe.getMethod().getId());
            dto.setMethodName(recipe.getMethod().getName());

            dto.setTitle(recipe.getTitle());
            dto.setGlobal(recipe.isGlobal());
            dto.setFavorite(exists);

            dto.setCreatedAt(recipe.getCreatedAt());
            dto.setUpdatedAt(recipe.getUpdatedAt());

            dto.setVersionId(version.getId());
            dto.setVersionNumber(version.getVersionNumber());
            dto.setCurrent(version.isCurrent());
            dto.setCoffeeAmount(version.getCoffeeAmount());
            dto.setWaterAmount(version.getWaterAmount());
            dto.setGrindSize(version.getGrindSize());
            dto.setBrewTimeSeconds(version.getBrewTimeSeconds());
            dto.setWaterTemperatureCelsius(version.getWaterTemperatureCelsius());
            dto.setRating(version.getRating());
            dto.setMethodPayload(version.getMethodPayload());
            dto.setVersionUpdatedAt(version.getUpdatedAt());

            List<WaterPourDTO> pourDTOs = waterPours.stream().map(p -> {
                WaterPourDTO pourDto = new WaterPourDTO();
                pourDto.setWaterAmountMl(p.getWaterAmount());
                pourDto.setTime(p.getTime());
                pourDto.setOrderIndex(p.getOrderIndex());
                return pourDto;
            }).toList();

            List<UUID> equipmentIds = recipeEquipments.stream()
                    .map(e -> e.getEquipment().getId())
                    .toList();

            dto.setWaterPours(pourDTOs);
            dto.setEquipmentIds(equipmentIds);

            return dto;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found.");
        }
    }


    @Transactional
    public RecipeVersionResponseDTO updateRecipe(UUID userId, UUID trackId, UpdateRecipeRequestDTO dto) {
       if(userId == null || dto == null || trackId == null) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There are missing fields");
       }
        boolean noScalarUpdates =
                dto.getTitle() == null
                        && dto.getCoffeeAmount() == null
                        && dto.getWaterAmount() == null
                        && dto.getGrindSize() == null
                        && dto.getBrewTimeSeconds() == null
                        && dto.getWaterTemperatureCelsius() == null
                        && dto.getRating() == null
                        && dto.getMethodPayload() == null;

        boolean noCollectionUpdates =
                dto.getWaterPours() == null
                        && dto.getEquipmentIds() == null;

        if (noScalarUpdates && noCollectionUpdates) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field is required for update.");
        }

       if(dto.getTitle() != null && dto.getTitle().isBlank()) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must not be empty.");
       }
       if(dto.getRating() != null){
           if(dto.getRating() < 1 || dto.getRating() > 5){
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating should be between 1 and 5");
           }
       }
       if(dto.getBrewTimeSeconds() != null && dto.getBrewTimeSeconds() < 0){
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time cannot be negative");
       }
       if(dto.getWaterTemperatureCelsius() != null && dto.getWaterTemperatureCelsius() < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Water temperature cannot be negative");
       }
        if (dto.getWaterPours() != null) {
            Set<Integer> seenOrderIndexes = new HashSet<>();

            for (WaterPourDTO p : dto.getWaterPours()) {
                if (p == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Water pour item must not be null.");
                }
                if (p.getWaterAmountMl() == null || p.getWaterAmountMl() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Water amount must be greater than 0.");
                }
                if (p.getTime() == null || p.getTime().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Water pour time must not be blank.");
                }
                if (p.getOrderIndex() == null || p.getOrderIndex() < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order index must be 0 or greater.");
                }
                if (!seenOrderIndexes.add(p.getOrderIndex())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate orderIndex in water pours.");
                }
            }
        }

        if (dto.getEquipmentIds() != null) {
            Set<UUID> seenEquipmentIds = new HashSet<>();

            for (UUID equipmentId : dto.getEquipmentIds()) {
                if (equipmentId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Equipment id must not be null.");
                }
                if (!seenEquipmentIds.add(equipmentId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate equipment id.");
                }
            }
        }


        RecipeTrack recipe = recipeTrackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found."));
        if(recipe.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found.");
        }
        if(recipe.getOwner().getId().equals(userId)) {
            RecipeVersion version = recipeVersionRepository.findByTrack_IdAndIsCurrentTrue(trackId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
            if(version.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found.");
            }
            String rawMethodPayload = dto.getMethodPayload();
            String normalizedPayloadString;
            if (rawMethodPayload != null && !rawMethodPayload.isBlank()) {
                MethodPayloadStrategy strategy = methodPayloadStrategyRegistry.getRequired(recipe.getMethod().getName());

                JsonNode payloadJson;
                try {
                    payloadJson = objectMapper.readTree(rawMethodPayload);
                } catch (JsonProcessingException e){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid methodPayload JSON.");
                }
                JsonNode normalizedPayload = strategy.validateAndNormalize(payloadJson);
                try {
                    normalizedPayloadString = objectMapper.writeValueAsString(normalizedPayload);
                } catch (JsonProcessingException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize methodPayload JSON.", e);
                }
            } else {
                normalizedPayloadString = version.getMethodPayload();
            }
            List<RecipeWaterPour> waterPours = recipeWaterPourRepository.findByRecipeVersion_IdOrderByOrderIndexAsc(version.getId());
            List<RecipeEquipment> recipeEquipments = recipeEquipmentRepository.findByRecipeVersion_Id(version.getId());

            RecipeVersion newVersion = new RecipeVersion();
            newVersion.setTrack(recipe);
            newVersion.setVersionNumber(version.getVersionNumber() + 1);
            newVersion.setCurrent(true);
            String updatedTitle = dto.getTitle() != null ? dto.getTitle().trim() : null;
            newVersion.setTitle(updatedTitle != null && !updatedTitle.isBlank() ? updatedTitle : version.getTitle());
            recipe.setTitle(newVersion.getTitle());
            newVersion.setCoffeeAmount(dto.getCoffeeAmount() != null ? dto.getCoffeeAmount() : version.getCoffeeAmount());
            newVersion.setWaterAmount(dto.getWaterAmount() != null ? dto.getWaterAmount() : version.getWaterAmount());
            newVersion.setGrindSize(dto.getGrindSize() != null ? dto.getGrindSize() : version.getGrindSize());
            newVersion.setBrewTimeSeconds(dto.getBrewTimeSeconds() != null ? dto.getBrewTimeSeconds() : version.getBrewTimeSeconds());
            newVersion.setWaterTemperatureCelsius(dto.getWaterTemperatureCelsius() != null ? dto.getWaterTemperatureCelsius() : version.getWaterTemperatureCelsius());
            newVersion.setRating(dto.getRating() != null ? dto.getRating() : version.getRating());
            newVersion.setMethodPayload(normalizedPayloadString);
            version.setCurrent(false);

            recipeVersionRepository.saveAndFlush(version);
            recipeTrackRepository.save(recipe);
            RecipeVersion saved = recipeVersionRepository.save(newVersion);

            List<WaterPourDTO> effectivePours =
                dto.getWaterPours() != null
                    ? dto.getWaterPours()
                    : waterPours.stream().map(p -> {
                        WaterPourDTO x = new WaterPourDTO();
                        x.setWaterAmountMl(p.getWaterAmount());
                        x.setTime(p.getTime());
                        x.setOrderIndex(p.getOrderIndex());
                        return x;
                }).toList();
            List<RecipeWaterPour> newPours = effectivePours.stream().map(p -> {
                RecipeWaterPour row = new RecipeWaterPour();
                row.setRecipeVersion(saved);
                row.setWaterAmount(p.getWaterAmountMl());
                row.setTime(p.getTime());
                row.setOrderIndex(p.getOrderIndex());
                return row;
            }).toList();
            recipeWaterPourRepository.saveAll(newPours);



            List<UUID> effectiveEquipmentIds =
                    dto.getEquipmentIds() != null
                        ? dto.getEquipmentIds()
                        : recipeEquipments.stream()
                                    .map(e -> e.getEquipment().getId())
                                            .toList();

            List<RecipeEquipment> newEquipments = effectiveEquipmentIds.stream().map(equipmentId -> {
                Equipment equipment = equipmentRepository.findById(equipmentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));

                RecipeEquipment row = new RecipeEquipment();
                row.setId(new RecipeEquipmentId(saved.getId(), equipment.getId()));
                row.setRecipeVersion(saved);
                row.setEquipment(equipment);
                return row;
            }).toList();

            recipeEquipmentRepository.saveAll(newEquipments);

            RecipeVersionResponseDTO answer = new RecipeVersionResponseDTO();
            answer.setTrackId(recipe.getId());
            answer.setVersionId(saved.getId());
            answer.setVersionNumber(saved.getVersionNumber());
            answer.setCurrent(saved.isCurrent());
            answer.setBeanId(recipe.getBean() != null ? recipe.getBean().getId() : null);
            answer.setMethodId(recipe.getMethod().getId());
            answer.setTitle(saved.getTitle());
            answer.setGlobal(recipe.isGlobal());
            answer.setUpdatedAt(saved.getUpdatedAt());

            return answer;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found");
        }

    }

    @Transactional
    public void deleteRecipe(UUID userId, UUID trackId) {
        if(userId == null || trackId == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There are missing fields");
        }

        RecipeTrack track = recipeTrackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));
        if(track.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found");
        }
        if(!track.getOwner().getId().equals(userId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found");
        }
        LocalDateTime now = LocalDateTime.now();
        track.setDeletedAt(now);
        List<RecipeVersion> versions = recipeVersionRepository.findByTrack_IdOrderByVersionNumberDesc(trackId);
        for(RecipeVersion version : versions){
            if(version.getDeletedAt() == null) {
                version.setDeletedAt(now);
                version.setCurrent(false);
            }
        }
        recipeVersionRepository.saveAll(versions);
        recipeTrackRepository.save(track);
    }

    @Transactional(readOnly = true)
    public List<VersionHistoryItemDTO> listRecipeVersions(UUID userId, UUID trackId) {
        if(userId == null || trackId == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There are missing fields");
        }

        RecipeTrack track = recipeTrackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));
        if(track.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found");
        }
        if(!track.getOwner().getId().equals(userId) && !track.isGlobal()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found");
        }
        List<RecipeVersion> versions = recipeVersionRepository.findByTrack_IdOrderByVersionNumberDesc(trackId);
        List<VersionHistoryItemDTO> ans = new ArrayList<>();
        for(RecipeVersion version : versions){
            if(version.getDeletedAt() != null) {
                continue;
            }
            VersionHistoryItemDTO curr = new VersionHistoryItemDTO();
            curr.setVersionId(version.getId());
            curr.setVersionNumber(version.getVersionNumber());
            curr.setCurrent(version.isCurrent());
            curr.setTitle(version.getTitle());
            curr.setRating(version.getRating());
            curr.setCreatedAt(version.getCreatedAt());
            curr.setUpdatedAt(version.getUpdatedAt());
            ans.add(curr);

        }
        return ans;
    }

    public MethodPayloadMetadataDTO getMetadata(UUID methodId) {
        if(methodId== null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "methodId is required");
        }
        BrewMethods method = brewMethodsRepository.findById(methodId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Method not found"));
        return methodPayloadStrategyRegistry.metadata(method.getName(), method.getName());
    }

}
