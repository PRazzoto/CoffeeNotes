package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.recipe.RecipeCreateDTO;
import com.example.coffeenotes.api.dto.recipe.RecipeResponseDTO;
import com.example.coffeenotes.api.dto.recipe.RecipeUpdateDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.domain.catalog.Recipe;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.BrewMethodsRepository;
import com.example.coffeenotes.feature.catalog.repository.RecipeRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
    private static final UUID RECIPE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID METHOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private BrewMethodsRepository brewMethodsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void create_whenBodyNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recipeService.create(USER_ID, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void create_whenValid_returnsDto() {
        RecipeCreateDTO body = new RecipeCreateDTO();
        body.setMethodId(METHOD_ID);
        body.setTitle("V60 Daily");
        body.setRating(5);
        body.setIsGlobal(true);

        User owner = new User(USER_ID, "p@example.com", "hash", "Patri", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        BrewMethods method = new BrewMethods(METHOD_ID, "V60", "Cone");
        Recipe saved = new Recipe();
        saved.setId(RECIPE_ID);
        saved.setOwner(owner);
        saved.setMethod(method);
        saved.setTitle("V60 Daily");
        saved.setRating(5);
        saved.setGlobal(true);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.of(method));
        when(recipeRepository.save(any())).thenReturn(saved);

        RecipeResponseDTO result = recipeService.create(USER_ID, body);

        assertEquals(RECIPE_ID, result.getId());
        assertEquals(USER_ID, result.getOwnerId());
        assertEquals(METHOD_ID, result.getMethodId());
        assertEquals("V60 Daily", result.getTitle());
        assertEquals(5, result.getRating());
        assertEquals(true, result.getIsGlobal());
    }

    @Test
    void listByUserId_whenNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recipeService.listByUserId(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void listByUserId_returnsDtos() {
        User owner = new User(USER_ID, "p@example.com", "hash", "Patri", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        BrewMethods method = new BrewMethods(METHOD_ID, "V60", "Cone");
        Recipe r = new Recipe();
        r.setId(RECIPE_ID);
        r.setOwner(owner);
        r.setMethod(method);
        r.setTitle("Morning");

        when(recipeRepository.findVisibleByUserId(USER_ID)).thenReturn(List.of(r));

        List<RecipeResponseDTO> result = recipeService.listByUserId(USER_ID);

        assertEquals(1, result.size());
        assertEquals(RECIPE_ID, result.get(0).getId());
        assertEquals(USER_ID, result.get(0).getOwnerId());
        assertEquals(METHOD_ID, result.get(0).getMethodId());
    }

    @Test
    void updateRecipe_whenNotOwner_throws404() {
        User owner = new User(OTHER_USER_ID, "o@example.com", "hash", "Other", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        Recipe existing = new Recipe();
        existing.setId(RECIPE_ID);
        existing.setOwner(owner);
        existing.setMethod(new BrewMethods(METHOD_ID, "V60", "Cone"));
        existing.setTitle("Old");

        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recipeService.updateRecipe(RECIPE_ID, new RecipeUpdateDTO(), USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateRecipe_whenValid_updatesAndReturnsDto() {
        User owner = new User(USER_ID, "p@example.com", "hash", "Patri", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        BrewMethods oldMethod = new BrewMethods(METHOD_ID, "V60", "Cone");
        UUID newMethodId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        BrewMethods newMethod = new BrewMethods(newMethodId, "AeroPress", "Immersion");

        Recipe existing = new Recipe();
        existing.setId(RECIPE_ID);
        existing.setOwner(owner);
        existing.setMethod(oldMethod);
        existing.setTitle("Old");
        existing.setGlobal(false);

        RecipeUpdateDTO body = new RecipeUpdateDTO();
        body.setMethodId(newMethodId);
        body.setTitle("New Title");
        body.setRating(4);
        body.setIsGlobal(true);

        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(existing));
        when(brewMethodsRepository.findById(newMethodId)).thenReturn(Optional.of(newMethod));
        when(recipeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeResponseDTO result = recipeService.updateRecipe(RECIPE_ID, body, USER_ID);

        assertEquals(RECIPE_ID, result.getId());
        assertEquals(newMethodId, result.getMethodId());
        assertEquals("New Title", result.getTitle());
        assertEquals(4, result.getRating());
        assertEquals(true, result.getIsGlobal());

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        assertEquals("New Title", captor.getValue().getTitle());
        assertEquals(newMethodId, captor.getValue().getMethod().getId());
    }

    @Test
    void delete_whenValid_setsDeletedAtAndSaves() {
        User owner = new User(USER_ID, "p@example.com", "hash", "Patri", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        Recipe recipe = new Recipe();
        recipe.setId(RECIPE_ID);
        recipe.setOwner(owner);
        recipe.setMethod(new BrewMethods(METHOD_ID, "V60", "Cone"));
        recipe.setTitle("To delete");

        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(recipe));

        recipeService.delete(RECIPE_ID, USER_ID);

        assertNotNull(recipe.getDeletedAt());
        verify(recipeRepository).save(recipe);
    }

    @Test
    void delete_whenUserIdNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recipeService.delete(RECIPE_ID, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(recipeRepository, never()).findById(any());
    }

    @Test
    void create_whenNegativeBrewTime_throws400() {
        RecipeCreateDTO body = new RecipeCreateDTO();
        body.setMethodId(METHOD_ID);
        body.setTitle("Test");
        body.setBrewTimeSeconds(-10);

        User owner = new User(USER_ID, "p@example.com", "hash", "Patri", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        BrewMethods method = new BrewMethods(METHOD_ID, "V60", "Cone");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.of(method));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recipeService.create(USER_ID, body)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Brew time should not be negative", ex.getReason());
    }

    @Test
    void create_whenNegativeTemperature_throws400() {
        RecipeCreateDTO body = new RecipeCreateDTO();
        body.setMethodId(METHOD_ID);
        body.setTitle("Test");
        body.setWaterTemperatureCelsius(-5);

        User owner = new User(USER_ID, "p@example.com", "hash", "Patri", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        BrewMethods method = new BrewMethods(METHOD_ID, "V60", "Cone");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(brewMethodsRepository.findById(METHOD_ID)).thenReturn(Optional.of(method));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recipeService.create(USER_ID, body)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Temperature of the water should not be below 0", ex.getReason());
    }

    @Test
    void updateRecipe_whenAllFieldsNull_throws400() {
        User owner = new User(USER_ID, "p@example.com", "hash", "Patri", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        Recipe existing = new Recipe();
        existing.setId(RECIPE_ID);
        existing.setOwner(owner);
        existing.setMethod(new BrewMethods(METHOD_ID, "V60", "Cone"));
        existing.setTitle("Existing");

        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recipeService.updateRecipe(RECIPE_ID, new RecipeUpdateDTO(), USER_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("At least one field is required for update", ex.getReason());
    }
}
