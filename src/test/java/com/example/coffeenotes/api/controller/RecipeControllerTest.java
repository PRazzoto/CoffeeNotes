package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.RecipeResponseDTO;
import com.example.coffeenotes.feature.catalog.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RECIPE_ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID RECIPE_ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @Test
    void getRecipes_returnsDtos() throws Exception {
        when(recipeService.listByUserId(USER_ID)).thenReturn(List.of(
                recipeResponse(RECIPE_ID_1, "Morning V60"),
                recipeResponse(RECIPE_ID_2, "Weekend AeroPress")
        ));

        mockMvc.perform(get("/api/recipe/getRecipes").param("userId", USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Morning V60", "Weekend AeroPress")));
    }

    @Test
    void createRecipe_returns201() throws Exception {
        when(recipeService.create(eq(USER_ID), any())).thenReturn(recipeResponse(RECIPE_ID_1, "New Recipe"));

        mockMvc.perform(post("/api/recipe/createRecipe")
                        .param("userId", USER_ID.toString())
                        .contentType("application/json")
                        .content("""
                                {
                                  "methodId": "33333333-3333-3333-3333-333333333333",
                                  "title": "New Recipe"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RECIPE_ID_1.toString()))
                .andExpect(jsonPath("$.title").value("New Recipe"));
    }

    @Test
    void updateRecipe_returns200() throws Exception {
        when(recipeService.updateRecipe(eq(RECIPE_ID_1), any(), eq(USER_ID)))
                .thenReturn(recipeResponse(RECIPE_ID_1, "Updated Recipe"));

        mockMvc.perform(patch("/api/recipe/updateRecipe")
                        .param("id", RECIPE_ID_1.toString())
                        .param("userId", USER_ID.toString())
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Updated Recipe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Recipe"));
    }

    @Test
    void deleteRecipe_returns204() throws Exception {
        mockMvc.perform(delete("/api/recipe/deleteRecipe/" + RECIPE_ID_1)
                        .param("userId", USER_ID.toString()))
                .andExpect(status().isNoContent());

        verify(recipeService).delete(RECIPE_ID_1, USER_ID);
    }

    @Test
    void createRecipe_whenServiceThrows400_returns400() throws Exception {
        when(recipeService.create(eq(USER_ID), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid"));

        mockMvc.perform(post("/api/recipe/createRecipe")
                        .param("userId", USER_ID.toString())
                        .contentType("application/json")
                        .content("""
                                {
                                  "methodId": "33333333-3333-3333-3333-333333333333"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRecipe_whenServiceThrows404_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"))
                .when(recipeService).delete(RECIPE_ID_2, USER_ID);

        mockMvc.perform(delete("/api/recipe/deleteRecipe/" + RECIPE_ID_2)
                        .param("userId", USER_ID.toString()))
                .andExpect(status().isNotFound());
    }

    private RecipeResponseDTO recipeResponse(UUID id, String title) {
        RecipeResponseDTO dto = new RecipeResponseDTO();
        dto.setId(id);
        dto.setOwnerId(USER_ID);
        dto.setMethodId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        dto.setTitle(title);
        dto.setIsGlobal(false);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}
