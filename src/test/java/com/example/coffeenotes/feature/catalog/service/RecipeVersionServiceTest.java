package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.recipe.*;
import com.example.coffeenotes.domain.catalog.*;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeVersionServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TRACK_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID METHOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BEAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID EQUIPMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private CoffeeBeanRepository coffeeBeanRepository;
    @Mock
    private BrewMethodsRepository brewMethodsRepository;
    @Mock
    private RecipeTrackRepository recipeTrackRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RecipeVersionRepository recipeVersionRepository;
    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private RecipeWaterPourRepository recipeWaterPourRepository;
    @Mock
    private RecipeEquipmentRepository recipeEquipmentRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private MethodPayloadStrategyRegistry methodPayloadStrategyRegistry;
    @Mock
    private MethodPayloadStrategy methodPayloadStrategy;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RecipeVersionService recipeVersionService;

    @Test
    void createRecipe_whenValid_returnsCreatedVersion() {
        User owner = user(USER_ID, "owner@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, false);
        BrewMethods method = method(METHOD_ID, "V60");

        CreateTrackRequestDTO dto = new CreateTrackRequestDTO();
        dto.setBeanId(BEAN_ID);
        dto.setMethodId(METHOD_ID);
        dto.setTitle("Morning");
        dto.setGlobal(true);

        RecipeTrack savedTrack = track(TRACK_ID, owner, bean, method, "Morning", true, null);
        RecipeVersion savedVersion = version(VERSION_ID, savedTrack, 1, true, "Morning", null);
        savedVersion.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(coffeeBeanRepository.findById(BEAN_ID)).thenReturn(Optional.of(bean));
        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.of(method));
        when(recipeTrackRepository.findByOwner_IdAndBean_IdAndMethod_IdAndDeletedAtIsNull(USER_ID, BEAN_ID, METHOD_ID))
                .thenReturn(Optional.empty());
        stubMethodPayloadFlow("{}");
        when(recipeTrackRepository.save(any())).thenReturn(savedTrack);
        when(recipeVersionRepository.save(any())).thenReturn(savedVersion);

        RecipeVersionResponseDTO out = recipeVersionService.createRecipe(USER_ID, dto);

        assertEquals(TRACK_ID, out.getTrackId());
        assertEquals(VERSION_ID, out.getVersionId());
        assertEquals(1, out.getVersionNumber());
        assertEquals(BEAN_ID, out.getBeanId());
        assertEquals(METHOD_ID, out.getMethodId());
        assertEquals("Morning", out.getTitle());
        assertTrue(out.isGlobal());
    }

    @Test
    void createRecipe_whenTrackAlreadyExists_throws409() {
        User owner = user(USER_ID, "owner@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, false);
        BrewMethods method = method(METHOD_ID, "V60");

        CreateTrackRequestDTO dto = new CreateTrackRequestDTO();
        dto.setBeanId(BEAN_ID);
        dto.setMethodId(METHOD_ID);
        dto.setTitle("Morning");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(coffeeBeanRepository.findById(BEAN_ID)).thenReturn(Optional.of(bean));
        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.of(method));
        when(recipeTrackRepository.findByOwner_IdAndBean_IdAndMethod_IdAndDeletedAtIsNull(USER_ID, BEAN_ID, METHOD_ID))
                .thenReturn(Optional.of(track(TRACK_ID, owner, bean, method, "Morning", false, null)));
        stubMethodPayloadFlow("{}");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recipeVersionService.createRecipe(USER_ID, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void listRecipes_whenFilteredByMethodAndFavorites_returnsExpectedPage() {
        User owner = user(USER_ID, "owner@test.com");
        User other = user(OTHER_USER_ID, "other@test.com");
        CoffeeBean bean2 = bean(UUID.randomUUID(), other, true);
        BrewMethods method2 = method(UUID.randomUUID(), "AeroPress");

        RecipeTrack t2 = track(UUID.randomUUID(), other, bean2, method2, "Global", true, null);

        Favorite favorite = new Favorite();
        favorite.setUser(owner);
        favorite.setRecipeTrack(t2);

        RecipeVersion v2 = version(UUID.randomUUID(), t2, 3, true, "Global", null);
        v2.setRating(5);
        v2.setUpdatedAt(LocalDateTime.now());

        RecipeFilterDTO filter = new RecipeFilterDTO();
        filter.setMethodId(method2.getId());
        filter.setFavoritesOnly(true);

        Pageable pageable = PageRequest.of(0, 10);
        Page<RecipeTrack> trackPage = new PageImpl<>(List.of(t2), pageable, 1);

        when(favoriteRepository.findByUser_Id(USER_ID)).thenReturn(List.of(favorite));
        when(recipeTrackRepository.findVisibleTracks(USER_ID, method2.getId(), null, true, pageable))
                .thenReturn(trackPage);
        when(recipeVersionRepository.findByTrack_IdInAndIsCurrentTrue(List.of(t2.getId())))
                .thenReturn(List.of(v2));

        Page<TrackSummaryResponseDTO> page = recipeVersionService.listRecipes(USER_ID, filter, pageable);

        assertEquals(1, page.getContent().size());
        TrackSummaryResponseDTO dto = page.getContent().get(0);
        assertEquals(t2.getId(), dto.getTrackId());
        assertEquals(t2.getBean().getId(), dto.getBeanId());
        assertTrue(dto.isFavorite());
        assertEquals(3, dto.getCurrentVersionNumber());
    }

    @Test
    void getRecipe_whenGlobalTrack_returnsDetails() {
        User owner = user(OTHER_USER_ID, "other@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, true);
        BrewMethods method = method(METHOD_ID, "V60");
        RecipeTrack track = track(TRACK_ID, owner, bean, method, "Global Track", true, null);

        RecipeVersion current = version(VERSION_ID, track, 2, true, "Global Track", null);
        current.setCoffeeAmount("15g");
        current.setWaterAmount("250ml");

        RecipeWaterPour pour = new RecipeWaterPour();
        pour.setRecipeVersion(current);
        pour.setWaterAmount(100);
        pour.setTime("00:30");
        pour.setOrderIndex(0);

        Equipment equipment = new Equipment();
        equipment.setId(EQUIPMENT_ID);
        RecipeEquipment recipeEquipment = new RecipeEquipment();
        recipeEquipment.setId(new RecipeEquipmentId(VERSION_ID, EQUIPMENT_ID));
        recipeEquipment.setRecipeVersion(current);
        recipeEquipment.setEquipment(equipment);

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(recipeVersionRepository.findByTrack_IdAndIsCurrentTrue(TRACK_ID)).thenReturn(Optional.of(current));
        when(recipeWaterPourRepository.findByRecipeVersion_IdOrderByOrderIndexAsc(VERSION_ID)).thenReturn(List.of(pour));
        when(recipeEquipmentRepository.findByRecipeVersion_Id(VERSION_ID)).thenReturn(List.of(recipeEquipment));
        when(favoriteRepository.existsByUser_IdAndRecipeTrack_Id(USER_ID, TRACK_ID)).thenReturn(true);

        TrackDetailsResponseDTO dto = recipeVersionService.getRecipe(USER_ID, TRACK_ID);

        assertEquals(TRACK_ID, dto.getTrackId());
        assertEquals(BEAN_ID, dto.getBeanId());
        assertEquals(VERSION_ID, dto.getVersionId());
        assertEquals(1, dto.getWaterPours().size());
        assertEquals(1, dto.getEquipmentIds().size());
        assertTrue(dto.isFavorite());
    }

    @Test
    void updateRecipe_whenValid_createsNextVersionAndChildren() {
        User owner = user(USER_ID, "owner@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, false);
        BrewMethods method = method(METHOD_ID, "V60");
        RecipeTrack track = track(TRACK_ID, owner, bean, method, "Old title", false, null);

        RecipeVersion current = version(UUID.randomUUID(), track, 1, true, "Old title", null);
        current.setCoffeeAmount("15g");
        current.setWaterAmount("250ml");
        current.setGrindSize("Medium");
        current.setBrewTimeSeconds(120);
        current.setWaterTemperatureCelsius(94);
        current.setRating(4);
        current.setMethodPayload("{}");

        RecipeWaterPour oldPour = new RecipeWaterPour();
        oldPour.setRecipeVersion(current);
        oldPour.setWaterAmount(100);
        oldPour.setTime("00:30");
        oldPour.setOrderIndex(0);

        Equipment equipment = new Equipment();
        equipment.setId(EQUIPMENT_ID);
        RecipeEquipment oldEquipment = new RecipeEquipment();
        oldEquipment.setId(new RecipeEquipmentId(current.getId(), EQUIPMENT_ID));
        oldEquipment.setRecipeVersion(current);
        oldEquipment.setEquipment(equipment);

        UpdateRecipeRequestDTO dto = new UpdateRecipeRequestDTO();
        dto.setTitle("New title");

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(recipeVersionRepository.findByTrack_IdAndIsCurrentTrue(TRACK_ID)).thenReturn(Optional.of(current));
        when(recipeWaterPourRepository.findByRecipeVersion_IdOrderByOrderIndexAsc(current.getId())).thenReturn(List.of(oldPour));
        when(recipeEquipmentRepository.findByRecipeVersion_Id(current.getId())).thenReturn(List.of(oldEquipment));
        when(recipeTrackRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
        when(recipeVersionRepository.save(any())).thenAnswer(invocation -> {
            RecipeVersion arg = invocation.getArgument(0);
            if (arg == current) {
                return arg;
            }
            arg.setId(VERSION_ID);
            arg.setUpdatedAt(LocalDateTime.now());
            return arg;
        });

        RecipeVersionResponseDTO out = recipeVersionService.updateRecipe(USER_ID, TRACK_ID, dto);

        assertEquals(TRACK_ID, out.getTrackId());
        assertEquals(VERSION_ID, out.getVersionId());
        assertEquals(2, out.getVersionNumber());
        assertEquals("New title", out.getTitle());
        assertFalse(current.isCurrent());

        verify(recipeWaterPourRepository).saveAll(any());
        verify(recipeEquipmentRepository).saveAll(any());
    }

    @Test
    void createRecipe_whenMethodPayloadInvalidJson_throws400() throws Exception {
        User owner = user(USER_ID, "owner@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, false);
        BrewMethods method = method(METHOD_ID, "V60");

        CreateTrackRequestDTO dto = new CreateTrackRequestDTO();
        dto.setBeanId(BEAN_ID);
        dto.setMethodId(METHOD_ID);
        dto.setTitle("Morning");
        dto.setMethodPayload("{bad json");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(coffeeBeanRepository.findById(BEAN_ID)).thenReturn(Optional.of(bean));
        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.of(method));
        when(methodPayloadStrategyRegistry.getRequired("V60")).thenReturn(methodPayloadStrategy);
        when(objectMapper.readTree("{bad json")).thenThrow(new JsonProcessingException("invalid json") { });

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recipeVersionService.createRecipe(USER_ID, dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(recipeTrackRepository, never()).save(any());
    }

    @Test
    void createRecipe_whenStrategyRejectsPayload_throws400() throws Exception {
        User owner = user(USER_ID, "owner@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, false);
        BrewMethods method = method(METHOD_ID, "V60");

        CreateTrackRequestDTO dto = new CreateTrackRequestDTO();
        dto.setBeanId(BEAN_ID);
        dto.setMethodId(METHOD_ID);
        dto.setTitle("Morning");
        dto.setMethodPayload("{\"filterShape\":\"invalid\"}");

        JsonNode node = new ObjectMapper().readTree(dto.getMethodPayload());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(coffeeBeanRepository.findById(BEAN_ID)).thenReturn(Optional.of(bean));
        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.of(method));
        when(methodPayloadStrategyRegistry.getRequired("V60")).thenReturn(methodPayloadStrategy);
        when(objectMapper.readTree(dto.getMethodPayload())).thenReturn(node);
        when(methodPayloadStrategy.validateAndNormalize(node))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid payload"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recipeVersionService.createRecipe(USER_ID, dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(recipeTrackRepository, never()).save(any());
    }

    @Test
    void updateRecipe_whenMethodPayloadProvidedAndInvalid_throws400() throws Exception {
        User owner = user(USER_ID, "owner@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, false);
        BrewMethods method = method(METHOD_ID, "V60");
        RecipeTrack track = track(TRACK_ID, owner, bean, method, "Old title", false, null);
        RecipeVersion current = version(UUID.randomUUID(), track, 1, true, "Old title", null);
        current.setMethodPayload("{}");

        UpdateRecipeRequestDTO dto = new UpdateRecipeRequestDTO();
        dto.setMethodPayload("{bad json");

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(recipeVersionRepository.findByTrack_IdAndIsCurrentTrue(TRACK_ID)).thenReturn(Optional.of(current));
        when(methodPayloadStrategyRegistry.getRequired("V60")).thenReturn(methodPayloadStrategy);
        when(objectMapper.readTree("{bad json")).thenThrow(new JsonProcessingException("invalid json") { });

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recipeVersionService.updateRecipe(USER_ID, TRACK_ID, dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(recipeVersionRepository, never()).save(any());
    }

    @Test
    void getMetadata_whenValidMethod_returnsStrategyMetadata() {
        BrewMethods method = method(METHOD_ID, "V60");
        MethodPayloadMetadataDTO metadata = new MethodPayloadMetadataDTO();
        metadata.setMethodKey("pour_over");
        metadata.setMethodName("V60");

        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.of(method));
        when(methodPayloadStrategyRegistry.metadata("V60", "V60")).thenReturn(metadata);

        MethodPayloadMetadataDTO out = recipeVersionService.getMetadata(METHOD_ID);

        assertEquals("pour_over", out.getMethodKey());
        assertEquals("V60", out.getMethodName());
    }

    @Test
    void getMetadata_whenMethodIdNull_throws400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recipeVersionService.getMetadata(null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getMetadata_whenMethodMissing_throws404() {
        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recipeVersionService.getMetadata(METHOD_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void deleteRecipe_whenValid_softDeletesTrackAndVersions() {
        User owner = user(USER_ID, "owner@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, false);
        BrewMethods method = method(METHOD_ID, "V60");
        RecipeTrack track = track(TRACK_ID, owner, bean, method, "Mine", false, null);

        RecipeVersion v1 = version(UUID.randomUUID(), track, 1, true, "v1", null);
        RecipeVersion v2 = version(UUID.randomUUID(), track, 2, false, "v2", null);

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(recipeVersionRepository.findByTrack_IdOrderByVersionNumberDesc(TRACK_ID)).thenReturn(List.of(v2, v1));

        recipeVersionService.deleteRecipe(USER_ID, TRACK_ID);

        assertNotNull(track.getDeletedAt());
        assertNotNull(v1.getDeletedAt());
        assertNotNull(v2.getDeletedAt());
        assertFalse(v1.isCurrent());

        verify(recipeVersionRepository).saveAll(any());
        verify(recipeTrackRepository).save(track);
    }

    @Test
    void listRecipeVersions_whenGlobalTrackForNonOwner_returnsNonDeletedHistory() {
        User owner = user(OTHER_USER_ID, "other@test.com");
        CoffeeBean bean = bean(BEAN_ID, owner, true);
        BrewMethods method = method(METHOD_ID, "V60");
        RecipeTrack track = track(TRACK_ID, owner, bean, method, "Global", true, null);

        RecipeVersion active = version(UUID.randomUUID(), track, 2, true, "v2", null);
        RecipeVersion deleted = version(UUID.randomUUID(), track, 1, false, "v1", LocalDateTime.now());

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(recipeVersionRepository.findByTrack_IdOrderByVersionNumberDesc(TRACK_ID)).thenReturn(List.of(active, deleted));

        List<VersionHistoryItemDTO> out = recipeVersionService.listRecipeVersions(USER_ID, TRACK_ID);

        assertEquals(1, out.size());
        assertEquals(2, out.get(0).getVersionNumber());
    }

    private User user(UUID id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setDisplayName("Name");
        u.setRole(Role.USER);
        return u;
    }

    private CoffeeBean bean(UUID id, User owner, boolean global) {
        CoffeeBean b = new CoffeeBean();
        b.setId(id);
        b.setOwner(owner);
        b.setName("Bean");
        b.setGlobal(global);
        return b;
    }

    private BrewMethods method(UUID id, String name) {
        BrewMethods m = new BrewMethods();
        m.setId(id);
        m.setName(name);
        return m;
    }

    private RecipeTrack track(UUID id, User owner, CoffeeBean bean, BrewMethods method, String title, boolean global, LocalDateTime deletedAt) {
        RecipeTrack t = new RecipeTrack();
        t.setId(id);
        t.setOwner(owner);
        t.setBean(bean);
        t.setMethod(method);
        t.setTitle(title);
        t.setGlobal(global);
        t.setDeletedAt(deletedAt);
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    private RecipeVersion version(UUID id, RecipeTrack track, int number, boolean current, String title, LocalDateTime deletedAt) {
        RecipeVersion v = new RecipeVersion();
        v.setId(id);
        v.setTrack(track);
        v.setVersionNumber(number);
        v.setCurrent(current);
        v.setTitle(title);
        v.setMethodPayload("{}");
        v.setDeletedAt(deletedAt);
        v.setCreatedAt(LocalDateTime.now());
        v.setUpdatedAt(LocalDateTime.now());
        return v;
    }

    private void stubMethodPayloadFlow(String rawPayload) {
        ObjectNode node = new ObjectMapper().createObjectNode();
        when(methodPayloadStrategyRegistry.getRequired("V60")).thenReturn(methodPayloadStrategy);
        try {
            when(objectMapper.readTree(rawPayload)).thenReturn(node);
            when(methodPayloadStrategy.validateAndNormalize(node)).thenReturn(node);
            when(objectMapper.writeValueAsString(node)).thenReturn("{}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
