package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.recipe.CreateTrackRequestDTO;
import com.example.coffeenotes.api.dto.recipe.RecipeVersionResponseDTO;
import com.example.coffeenotes.api.dto.recipe.UpdateRecipeRequestDTO;
import com.example.coffeenotes.api.dto.recipe.WaterPourDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.catalog.recipe.RecipeEquipment;
import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import com.example.coffeenotes.domain.catalog.recipe.RecipeVersion;
import com.example.coffeenotes.domain.catalog.recipe.RecipeWaterPour;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.BrewMethodsRepository;
import com.example.coffeenotes.feature.catalog.repository.CoffeeBeanRepository;
import com.example.coffeenotes.feature.catalog.repository.EquipmentRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeEquipmentRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeTrackRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeVersionRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeWaterPourRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RecipeVersionServiceIntegrationTest {

    @Autowired
    private RecipeVersionService recipeVersionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Autowired
    private BrewMethodsRepository brewMethodsRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private RecipeTrackRepository recipeTrackRepository;

    @Autowired
    private RecipeVersionRepository recipeVersionRepository;

    @Autowired
    private RecipeWaterPourRepository recipeWaterPourRepository;

    @Autowired
    private RecipeEquipmentRepository recipeEquipmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createRecipe_persistsTrackInitialVersionAndNormalizedPayload() throws Exception {
        User owner = persistedUser();
        CoffeeBean bean = persistedBean(owner, false);
        BrewMethods method = persistedMethod("V60");

        CreateTrackRequestDTO dto = new CreateTrackRequestDTO();
        dto.setBeanId(bean.getId());
        dto.setMethodId(method.getId());
        dto.setTitle("  Morning V60  ");
        dto.setGlobal(true);
        dto.setMethodPayload("""
                {
                  "filterShape": "CONE",
                  "dripperModel": "  Origami  "
                }
                """);

        RecipeVersionResponseDTO response = recipeVersionService.createRecipe(owner.getId(), dto);

        RecipeTrack savedTrack = recipeTrackRepository.findById(response.getTrackId()).orElseThrow();
        RecipeVersion currentVersion = recipeVersionRepository
                .findByTrack_IdAndIsCurrentTrue(savedTrack.getId())
                .orElseThrow();
        JsonNode payload = objectMapper.readTree(currentVersion.getMethodPayload());

        assertEquals("Morning V60", savedTrack.getTitle());
        assertTrue(savedTrack.isGlobal());
        assertEquals(bean.getId(), savedTrack.getBean().getId());
        assertEquals(method.getId(), savedTrack.getMethod().getId());

        assertEquals(1, currentVersion.getVersionNumber());
        assertTrue(currentVersion.isCurrent());
        assertEquals("Morning V60", currentVersion.getTitle());
        assertEquals("cone", payload.get("filterShape").asText());
        assertEquals("Origami", payload.get("dripperModel").asText());
        assertTrue(recipeWaterPourRepository.findByRecipeVersion_IdOrderByOrderIndexAsc(currentVersion.getId()).isEmpty());
        assertTrue(recipeEquipmentRepository.findByRecipeVersion_Id(currentVersion.getId()).isEmpty());
    }

    @Test
    void updateRecipe_createsNextVersionKeepsSingleCurrentAndBindsChildrenToNewVersion() throws Exception {
        User owner = persistedUser();
        CoffeeBean bean = persistedBean(owner, false);
        BrewMethods method = persistedMethod("V60");
        Equipment grinder = persistedEquipment("Grinder");
        Equipment kettle = persistedEquipment("Kettle");

        RecipeVersionResponseDTO created = recipeVersionService.createRecipe(owner.getId(),
                createTrackRequest(bean.getId(), method.getId(), "Morning Brew",
                        "{\"filterShape\":\"cone\"}"));

        RecipeVersion initialVersion = recipeVersionRepository
                .findByTrack_IdAndIsCurrentTrue(created.getTrackId())
                .orElseThrow();

        UpdateRecipeRequestDTO update = new UpdateRecipeRequestDTO();
        update.setTitle("Evening Brew");
        update.setCoffeeAmount("18g");
        update.setWaterAmount("300ml");
        update.setGrindSize("Medium-Fine");
        update.setBrewTimeSeconds(210);
        update.setWaterTemperatureCelsius(93);
        update.setRating(5);
        update.setMethodPayload("""
                {
                  "filterShape": "WAVE",
                  "pourStyle": "PULSE"
                }
                """);
        update.setWaterPours(List.of(
                waterPour(120, "00:30", 0),
                waterPour(180, "01:15", 1)
        ));
        update.setEquipmentIds(List.of(grinder.getId(), kettle.getId()));

        RecipeVersionResponseDTO updated = recipeVersionService.updateRecipe(owner.getId(), created.getTrackId(), update);

        List<RecipeVersion> versions = recipeVersionRepository.findByTrack_IdOrderByVersionNumberDesc(created.getTrackId());
        RecipeVersion currentVersion = recipeVersionRepository
                .findByTrack_IdAndIsCurrentTrue(created.getTrackId())
                .orElseThrow();
        RecipeTrack updatedTrack = recipeTrackRepository.findById(created.getTrackId()).orElseThrow();
        JsonNode payload = objectMapper.readTree(currentVersion.getMethodPayload());
        List<RecipeWaterPour> pours = recipeWaterPourRepository.findByRecipeVersion_IdOrderByOrderIndexAsc(currentVersion.getId());
        Set<UUID> equipmentIds = recipeEquipmentRepository.findByRecipeVersion_Id(currentVersion.getId()).stream()
                .map(RecipeEquipment::getEquipment)
                .map(Equipment::getId)
                .collect(Collectors.toSet());

        assertEquals(2, updated.getVersionNumber());
        assertEquals(currentVersion.getId(), updated.getVersionId());
        assertEquals(2, versions.size());
        assertEquals(1, versions.stream().filter(RecipeVersion::isCurrent).count());
        assertFalse(versions.stream()
                .filter(version -> version.getId().equals(initialVersion.getId()))
                .findFirst()
                .orElseThrow()
                .isCurrent());

        assertEquals("Evening Brew", updatedTrack.getTitle());
        assertEquals("Evening Brew", currentVersion.getTitle());
        assertEquals("18g", currentVersion.getCoffeeAmount());
        assertEquals("300ml", currentVersion.getWaterAmount());
        assertEquals("Medium-Fine", currentVersion.getGrindSize());
        assertEquals(210, currentVersion.getBrewTimeSeconds());
        assertEquals(93, currentVersion.getWaterTemperatureCelsius());
        assertEquals(5, currentVersion.getRating());
        assertEquals("wave", payload.get("filterShape").asText());
        assertEquals("pulse", payload.get("pourStyle").asText());

        assertEquals(2, pours.size());
        assertEquals(List.of(0, 1), pours.stream().map(RecipeWaterPour::getOrderIndex).toList());
        assertEquals(List.of(120, 180), pours.stream().map(RecipeWaterPour::getWaterAmount).toList());
        assertEquals(Set.of(grinder.getId(), kettle.getId()), equipmentIds);

        assertTrue(recipeWaterPourRepository.findByRecipeVersion_IdOrderByOrderIndexAsc(initialVersion.getId()).isEmpty());
        assertTrue(recipeEquipmentRepository.findByRecipeVersion_Id(initialVersion.getId()).isEmpty());
    }

    @Test
    void deleteRecipe_softDeletesTrackAndVersionsWhileKeepingVersionChildrenAttached() {
        User owner = persistedUser();
        CoffeeBean bean = persistedBean(owner, false);
        BrewMethods method = persistedMethod("V60");
        Equipment server = persistedEquipment("Server");

        RecipeVersionResponseDTO created = recipeVersionService.createRecipe(owner.getId(),
                createTrackRequest(bean.getId(), method.getId(), "Archive Brew", "{}"));

        UpdateRecipeRequestDTO update = new UpdateRecipeRequestDTO();
        update.setTitle("Archive Brew V2");
        update.setWaterPours(List.of(
                waterPour(150, "00:40", 0),
                waterPour(160, "01:20", 1)
        ));
        update.setEquipmentIds(List.of(server.getId()));

        RecipeVersionResponseDTO updated = recipeVersionService.updateRecipe(owner.getId(), created.getTrackId(), update);

        recipeVersionService.deleteRecipe(owner.getId(), created.getTrackId());

        RecipeTrack deletedTrack = recipeTrackRepository.findById(created.getTrackId()).orElseThrow();
        List<RecipeVersion> deletedVersions = recipeVersionRepository.findByTrack_IdOrderByVersionNumberDesc(created.getTrackId());
        List<RecipeWaterPour> retainedPours = recipeWaterPourRepository
                .findByRecipeVersion_IdOrderByOrderIndexAsc(updated.getVersionId());
        List<RecipeEquipment> retainedEquipment = recipeEquipmentRepository.findByRecipeVersion_Id(updated.getVersionId());

        assertNotNull(deletedTrack.getDeletedAt());
        assertEquals(2, deletedVersions.size());
        assertTrue(deletedVersions.stream().allMatch(version -> version.getDeletedAt() != null));
        assertTrue(deletedVersions.stream().noneMatch(RecipeVersion::isCurrent));

        assertEquals(2, retainedPours.size());
        assertEquals(1, retainedEquipment.size());
        assertEquals(server.getId(), retainedEquipment.get(0).getEquipment().getId());
    }

    private User persistedUser() {
        User user = new User();
        user.setEmail("integration-" + UUID.randomUUID() + "@coffee.test");
        user.setPasswordHash("hashed-password");
        user.setDisplayName("Integration User");
        user.setRole(Role.USER);
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

    private CreateTrackRequestDTO createTrackRequest(UUID beanId, UUID methodId, String title, String methodPayload) {
        CreateTrackRequestDTO dto = new CreateTrackRequestDTO();
        dto.setBeanId(beanId);
        dto.setMethodId(methodId);
        dto.setTitle(title);
        dto.setGlobal(false);
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
