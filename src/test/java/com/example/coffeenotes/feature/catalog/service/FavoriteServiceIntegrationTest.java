package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.recipe.CreateTrackRequestDTO;
import com.example.coffeenotes.api.dto.recipe.FavoriteResponseDTO;
import com.example.coffeenotes.api.dto.recipe.TrackSummaryResponseDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.catalog.Favorite;
import com.example.coffeenotes.domain.catalog.FavoriteId;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.BrewMethodsRepository;
import com.example.coffeenotes.feature.catalog.repository.CoffeeBeanRepository;
import com.example.coffeenotes.feature.catalog.repository.FavoriteRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeTrackRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FavoriteServiceIntegrationTest {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private RecipeVersionService recipeVersionService;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private RecipeTrackRepository recipeTrackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Autowired
    private BrewMethodsRepository brewMethodsRepository;

    @Test
    void addFavorite_andRemoveFavorite_areIdempotentAgainstPersistedRows() {
        User viewer = persistedUser();
        User owner = persistedUser();
        BrewMethods method = persistedMethod("V60");
        CoffeeBean bean = persistedBean(owner, false);

        UUID trackId = recipeVersionService.createRecipe(
                owner.getId(),
                createTrackRequest(bean.getId(), method.getId(), "Global Favorite", true, "{\"filterShape\":\"cone\"}")
        ).getTrackId();

        FavoriteResponseDTO firstAdd = favoriteService.addFavorite(viewer.getId(), trackId);
        FavoriteResponseDTO secondAdd = favoriteService.addFavorite(viewer.getId(), trackId);

        List<Favorite> persistedAfterAdd = favoriteRepository.findByUser_Id(viewer.getId());

        assertTrue(firstAdd.isFavorite());
        assertTrue(secondAdd.isFavorite());
        assertEquals(trackId, firstAdd.getTrackId());
        assertEquals(trackId, secondAdd.getTrackId());
        assertEquals(1, persistedAfterAdd.size());
        assertEquals(trackId, persistedAfterAdd.get(0).getRecipeTrack().getId());

        FavoriteResponseDTO firstRemove = favoriteService.removeFavorite(viewer.getId(), trackId);
        FavoriteResponseDTO secondRemove = favoriteService.removeFavorite(viewer.getId(), trackId);

        assertFalse(firstRemove.isFavorite());
        assertFalse(secondRemove.isFavorite());
        assertEquals(trackId, firstRemove.getTrackId());
        assertEquals(trackId, secondRemove.getTrackId());
        assertTrue(favoriteRepository.findByUser_Id(viewer.getId()).isEmpty());
    }

    @Test
    void listFavoriteRecipes_returnsOnlyActiveVisibleTracks() {
        User viewer = persistedUser();
        User owner = persistedUser();

        BrewMethods method = persistedMethod("V60");
        CoffeeBean viewerBean = persistedBean(viewer, false);
        CoffeeBean ownerBeanGlobal = persistedBean(owner, false);
        CoffeeBean ownerBeanPrivate = persistedBean(owner, false);

        UUID ownTrackId = recipeVersionService.createRecipe(
                viewer.getId(),
                createTrackRequest(viewerBean.getId(), method.getId(), "Own Favorite", false, "{\"filterShape\":\"cone\"}")
        ).getTrackId();

        UUID deletedGlobalTrackId = recipeVersionService.createRecipe(
                owner.getId(),
                createTrackRequest(ownerBeanGlobal.getId(), method.getId(), "Deleted Global", true, "{\"filterShape\":\"cone\"}")
        ).getTrackId();

        UUID hiddenPrivateTrackId = recipeVersionService.createRecipe(
                owner.getId(),
                createTrackRequest(ownerBeanPrivate.getId(), method.getId(), "Hidden Private", false, "{\"filterShape\":\"cone\"}")
        ).getTrackId();

        favoriteService.addFavorite(viewer.getId(), ownTrackId);
        favoriteService.addFavorite(viewer.getId(), deletedGlobalTrackId);

        User persistedViewer = userRepository.findById(viewer.getId()).orElseThrow();
        RecipeTrack hiddenTrack = recipeTrackRepository.findById(hiddenPrivateTrackId).orElseThrow();
        Favorite hiddenFavorite = new Favorite();
        hiddenFavorite.setId(new FavoriteId(viewer.getId(), hiddenPrivateTrackId));
        hiddenFavorite.setUser(persistedViewer);
        hiddenFavorite.setRecipeTrack(hiddenTrack);
        favoriteRepository.saveAndFlush(hiddenFavorite);

        recipeVersionService.deleteRecipe(owner.getId(), deletedGlobalTrackId);

        List<TrackSummaryResponseDTO> favorites = favoriteService.listFavoriteRecipes(viewer.getId());

        assertEquals(1, favorites.size());
        assertEquals(ownTrackId, favorites.get(0).getTrackId());
        assertEquals("Own Favorite", favorites.get(0).getTitle());
        assertTrue(favorites.get(0).isFavorite());
    }

    private User persistedUser() {
        User user = new User();
        user.setEmail("integration-favorite-" + UUID.randomUUID() + "@coffee.test");
        user.setPasswordHash("hashed-password");
        user.setDisplayName("Integration Favorite User");
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

    private CreateTrackRequestDTO createTrackRequest(UUID beanId, UUID methodId, String title, boolean global, String methodPayload) {
        CreateTrackRequestDTO dto = new CreateTrackRequestDTO();
        dto.setBeanId(beanId);
        dto.setMethodId(methodId);
        dto.setTitle(title);
        dto.setGlobal(global);
        dto.setMethodPayload(methodPayload);
        return dto;
    }
}
