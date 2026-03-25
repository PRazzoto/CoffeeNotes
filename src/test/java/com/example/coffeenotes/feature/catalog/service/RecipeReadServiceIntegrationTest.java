package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.recipe.CreateTrackRequestDTO;
import com.example.coffeenotes.api.dto.recipe.RecipeFilterDTO;
import com.example.coffeenotes.api.dto.recipe.RecipeVersionResponseDTO;
import com.example.coffeenotes.api.dto.recipe.TrackDetailsResponseDTO;
import com.example.coffeenotes.api.dto.recipe.TrackSummaryResponseDTO;
import com.example.coffeenotes.api.dto.recipe.UpdateRecipeRequestDTO;
import com.example.coffeenotes.api.dto.recipe.VersionHistoryItemDTO;
import com.example.coffeenotes.api.dto.recipe.WaterPourDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.BrewMethodsRepository;
import com.example.coffeenotes.feature.catalog.repository.CoffeeBeanRepository;
import com.example.coffeenotes.feature.catalog.repository.EquipmentRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RecipeReadServiceIntegrationTest {

    @Autowired
    private RecipeVersionService recipeVersionService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Autowired
    private BrewMethodsRepository brewMethodsRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listRecipes_returnsVisibleCurrentSnapshotsAndFavoriteFlags() {
        User viewer = persistedUser();
        User other = persistedAdminUser();
        String marker = "scope-" + UUID.randomUUID();

        BrewMethods pourOver = persistedMethod("V60");
        BrewMethods aeroPress = persistedMethod("AeroPress");

        CoffeeBean viewerBean = persistedBean(viewer, false);
        CoffeeBean otherBeanGlobalTrack = persistedBean(other, false);
        CoffeeBean otherBeanPrivateTrack = persistedBean(other, false);

        RecipeVersionResponseDTO ownTrack = recipeVersionService.createRecipe(
                viewer.getId(),
                createTrackRequest(viewerBean.getId(), pourOver.getId(), "Viewer V60 " + marker, false,
                        "{\"filterShape\":\"cone\"}")
        );

        UpdateRecipeRequestDTO ownUpdate = new UpdateRecipeRequestDTO();
        ownUpdate.setTitle("Viewer V60 v2 " + marker);
        ownUpdate.setRating(4);
        recipeVersionService.updateRecipe(viewer.getId(), ownTrack.getTrackId(), ownUpdate);

        RecipeVersionResponseDTO globalTrack = recipeVersionService.createRecipe(
                other.getId(),
                createTrackRequest(otherBeanGlobalTrack.getId(), aeroPress.getId(), "Global Aero " + marker, true,
                        "{\"steepTimeSeconds\":60,\"pressSeconds\":30,\"orientation\":\"standard\"}")
        );

        UpdateRecipeRequestDTO globalUpdate = new UpdateRecipeRequestDTO();
        globalUpdate.setTitle("Global Aero v2 " + marker);
        globalUpdate.setRating(5);
        recipeVersionService.updateRecipe(other.getId(), globalTrack.getTrackId(), globalUpdate);

        RecipeVersionResponseDTO hiddenTrack = recipeVersionService.createRecipe(
                other.getId(),
                createTrackRequest(otherBeanPrivateTrack.getId(), pourOver.getId(), "Hidden Private " + marker, false,
                        "{\"filterShape\":\"cone\"}")
        );

        favoriteService.addFavorite(viewer.getId(), globalTrack.getTrackId());

        RecipeFilterDTO scopedFilter = new RecipeFilterDTO();
        scopedFilter.setQ(marker);
        Page<TrackSummaryResponseDTO> page = recipeVersionService.listRecipes(
                viewer.getId(),
                scopedFilter,
                PageRequest.of(0, 10)
        );

        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getContent().size());

        Map<UUID, TrackSummaryResponseDTO> byId = page.getContent().stream()
                .collect(Collectors.toMap(TrackSummaryResponseDTO::getTrackId, Function.identity()));

        assertTrue(byId.containsKey(ownTrack.getTrackId()));
        assertTrue(byId.containsKey(globalTrack.getTrackId()));
        assertFalse(byId.containsKey(hiddenTrack.getTrackId()));

        TrackSummaryResponseDTO ownSummary = byId.get(ownTrack.getTrackId());
        assertEquals(viewerBean.getId(), ownSummary.getBeanId());
        assertEquals(viewerBean.getName(), ownSummary.getBeanName());
        assertEquals("Viewer V60 v2 " + marker, ownSummary.getTitle());
        assertEquals(pourOver.getId(), ownSummary.getMethodId());
        assertEquals("V60", ownSummary.getMethodName());
        assertEquals(2, ownSummary.getCurrentVersionNumber());
        assertEquals(4, ownSummary.getRating());
        assertFalse(ownSummary.isGlobal());
        assertFalse(ownSummary.isFavorite());
        assertNotNull(ownSummary.getUpdatedAt());

        TrackSummaryResponseDTO globalSummary = byId.get(globalTrack.getTrackId());
        assertEquals(otherBeanGlobalTrack.getId(), globalSummary.getBeanId());
        assertEquals("Global Aero v2 " + marker, globalSummary.getTitle());
        assertEquals(aeroPress.getId(), globalSummary.getMethodId());
        assertEquals("AeroPress", globalSummary.getMethodName());
        assertEquals(2, globalSummary.getCurrentVersionNumber());
        assertEquals(5, globalSummary.getRating());
        assertTrue(globalSummary.isGlobal());
        assertTrue(globalSummary.isFavorite());
        assertNotNull(globalSummary.getUpdatedAt());
    }

    @Test
    void listRecipes_appliesFavoritesOnlyIsGlobalAndMethodFilters() {
        User viewer = persistedUser();
        User other = persistedAdminUser();
        String marker = "scope-" + UUID.randomUUID();

        BrewMethods pourOver = persistedMethod("V60");
        BrewMethods aeroPress = persistedMethod("AeroPress");

        CoffeeBean viewerBean = persistedBean(viewer, false);
        CoffeeBean globalBean = persistedBean(other, false);

        RecipeVersionResponseDTO ownTrack = recipeVersionService.createRecipe(
                viewer.getId(),
                createTrackRequest(viewerBean.getId(), pourOver.getId(), "Viewer V60 " + marker, false,
                        "{\"filterShape\":\"cone\"}")
        );
        RecipeVersionResponseDTO globalTrack = recipeVersionService.createRecipe(
                other.getId(),
                createTrackRequest(globalBean.getId(), aeroPress.getId(), "Global Aero " + marker, true,
                        "{\"steepTimeSeconds\":60,\"pressSeconds\":30,\"orientation\":\"standard\"}")
        );

        favoriteService.addFavorite(viewer.getId(), globalTrack.getTrackId());

        RecipeFilterDTO favoritesOnly = new RecipeFilterDTO();
        favoritesOnly.setFavoritesOnly(true);
        favoritesOnly.setQ(marker);

        Page<TrackSummaryResponseDTO> favoritesPage = recipeVersionService.listRecipes(
                viewer.getId(),
                favoritesOnly,
                PageRequest.of(0, 10)
        );

        assertEquals(1, favoritesPage.getTotalElements());
        assertEquals(globalTrack.getTrackId(), favoritesPage.getContent().get(0).getTrackId());

        RecipeFilterDTO globalsOnly = new RecipeFilterDTO();
        globalsOnly.setIsGlobal(true);
        globalsOnly.setQ(marker);

        Page<TrackSummaryResponseDTO> globalsPage = recipeVersionService.listRecipes(
                viewer.getId(),
                globalsOnly,
                PageRequest.of(0, 10)
        );

        assertEquals(1, globalsPage.getTotalElements());
        assertEquals(globalTrack.getTrackId(), globalsPage.getContent().get(0).getTrackId());

        RecipeFilterDTO methodOnly = new RecipeFilterDTO();
        methodOnly.setMethodId(pourOver.getId());
        methodOnly.setQ(marker);

        Page<TrackSummaryResponseDTO> methodPage = recipeVersionService.listRecipes(
                viewer.getId(),
                methodOnly,
                PageRequest.of(0, 10)
        );

        assertEquals(1, methodPage.getTotalElements());
        assertEquals(ownTrack.getTrackId(), methodPage.getContent().get(0).getTrackId());
    }

    @Test
    void getRecipe_returnsCurrentVersionChildrenForVisibleGlobalTrack() throws Exception {
        User viewer = persistedUser();
        User owner = persistedAdminUser();

        BrewMethods method = persistedMethod("V60");
        CoffeeBean bean = persistedBean(owner, false);
        Equipment grinder = persistedEquipment("Grinder");

        RecipeVersionResponseDTO created = recipeVersionService.createRecipe(
                owner.getId(),
                createTrackRequest(bean.getId(), method.getId(), "Shared Brew", true,
                        "{\"filterShape\":\"CONE\",\"dripperModel\":\" Origami \"}")
        );

        UpdateRecipeRequestDTO update = new UpdateRecipeRequestDTO();
        update.setTitle("Shared Brew v2");
        update.setRating(5);
        update.setMethodPayload("""
                {
                  "filterShape": "WAVE",
                  "dripperModel": "  Glass  "
                }
                """);
        update.setWaterPours(List.of(
                waterPour(110, "00:25", 0),
                waterPour(190, "01:10", 1)
        ));
        update.setEquipmentIds(List.of(grinder.getId()));
        recipeVersionService.updateRecipe(owner.getId(), created.getTrackId(), update);

        favoriteService.addFavorite(viewer.getId(), created.getTrackId());

        TrackDetailsResponseDTO details = recipeVersionService.getRecipe(viewer.getId(), created.getTrackId());
        JsonNode payload = objectMapper.readTree(details.getMethodPayload());

        assertEquals(created.getTrackId(), details.getTrackId());
        assertEquals(bean.getId(), details.getBeanId());
        assertEquals(bean.getName(), details.getBeanName());
        assertEquals(method.getId(), details.getMethodId());
        assertEquals("V60", details.getMethodName());
        assertEquals("Shared Brew v2", details.getTitle());
        assertTrue(details.isGlobal());
        assertTrue(details.isFavorite());
        assertEquals(2, details.getVersionNumber());
        assertTrue(details.isCurrent());
        assertEquals(5, details.getRating());
        assertEquals("wave", payload.get("filterShape").asText());
        assertEquals("Glass", payload.get("dripperModel").asText());
        assertEquals(2, details.getWaterPours().size());
        assertEquals(List.of(0, 1), details.getWaterPours().stream().map(WaterPourDTO::getOrderIndex).toList());
        assertEquals(List.of(110, 190), details.getWaterPours().stream().map(WaterPourDTO::getWaterAmountMl).toList());
        assertEquals(1, details.getEquipmentIds().size());
        assertEquals(grinder.getId(), details.getEquipmentIds().get(0));
    }

    @Test
    void listRecipeVersions_returnsVisibleHistoryInDescendingVersionOrder() {
        User viewer = persistedUser();
        User owner = persistedAdminUser();

        BrewMethods method = persistedMethod("V60");
        CoffeeBean bean = persistedBean(owner, false);

        RecipeVersionResponseDTO created = recipeVersionService.createRecipe(
                owner.getId(),
                createTrackRequest(bean.getId(), method.getId(), "History Brew", true,
                        "{\"filterShape\":\"cone\"}")
        );

        UpdateRecipeRequestDTO second = new UpdateRecipeRequestDTO();
        second.setTitle("History Brew v2");
        second.setRating(3);
        recipeVersionService.updateRecipe(owner.getId(), created.getTrackId(), second);

        UpdateRecipeRequestDTO third = new UpdateRecipeRequestDTO();
        third.setTitle("History Brew v3");
        third.setRating(5);
        recipeVersionService.updateRecipe(owner.getId(), created.getTrackId(), third);

        List<VersionHistoryItemDTO> history = recipeVersionService.listRecipeVersions(viewer.getId(), created.getTrackId());

        assertEquals(3, history.size());
        assertEquals(List.of(3, 2, 1), history.stream().map(VersionHistoryItemDTO::getVersionNumber).toList());
        assertEquals(List.of("History Brew v3", "History Brew v2", "History Brew"), history.stream().map(VersionHistoryItemDTO::getTitle).toList());
        assertEquals(List.of(true, false, false), history.stream().map(VersionHistoryItemDTO::isCurrent).toList());
        assertEquals(5, history.get(0).getRating());
        assertEquals(3, history.get(1).getRating());
        assertNull(history.get(2).getRating());
    }

    private User persistedUser() {
        User user = new User();
        user.setEmail("integration-read-" + UUID.randomUUID() + "@coffee.test");
        user.setPasswordHash("hashed-password");
        user.setDisplayName("Integration Read User");
        user.setRole(Role.USER);
        return userRepository.saveAndFlush(user);
    }

    private User persistedAdminUser() {
        User user = new User();
        user.setEmail("integration-read-admin-" + UUID.randomUUID() + "@coffee.test");
        user.setPasswordHash("hashed-password");
        user.setDisplayName("Integration Read Admin");
        user.setRole(Role.ADMIN);
        return userRepository.saveAndFlush(user);
    }

    private CoffeeBean persistedBean(User owner, boolean global) {
        CoffeeBean bean = new CoffeeBean();
        bean.setOwner(owner);
        bean.setName("Bean " + UUID.randomUUID());
        bean.setGlobal(global);
        bean.setRoaster("April");
        bean.setOrigin("Ethiopia");
        bean.setProcess("Washed");
        bean.setNotes("Floral");
        return coffeeBeanRepository.saveAndFlush(bean);
    }

    private BrewMethods persistedMethod(String name) {
        BrewMethods method = new BrewMethods();
        method.setName(name);
        method.setDescription(name + " method");
        return brewMethodsRepository.saveAndFlush(method);
    }

    private Equipment persistedEquipment(String name) {
        Equipment equipment = new Equipment();
        equipment.setName(name + " " + UUID.randomUUID());
        equipment.setDescription("Integration test equipment");
        return equipmentRepository.saveAndFlush(equipment);
    }

    private CreateTrackRequestDTO createTrackRequest(UUID beanId, UUID methodId, String title, boolean global, String methodPayload) {
        CreateTrackRequestDTO dto = new CreateTrackRequestDTO();
        dto.setBeanId(beanId);
        dto.setMethodId(methodId);
        dto.setTitle(title);
        dto.setGlobal(global);
        dto.setMethodPayload(methodPayload);
        return dto;
    }

    private WaterPourDTO waterPour(int waterAmountMl, String time, int orderIndex) {
        WaterPourDTO dto = new WaterPourDTO();
        dto.setWaterAmountMl(waterAmountMl);
        dto.setTime(time);
        dto.setOrderIndex(orderIndex);
        return dto;
    }
}
