package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.recipe.*;
import com.example.coffeenotes.config.SecurityConfig;
import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodFieldMetadataDTO;
import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.example.coffeenotes.feature.catalog.service.RecipeVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class RecipeControllerTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TRACK_ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TRACK_ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID METHOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BEAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeVersionService recipeService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getRecipes_returnsDtos() throws Exception {
        TrackSummaryResponseDTO item = new TrackSummaryResponseDTO();
        item.setTrackId(TRACK_ID_1);
        item.setTitle("Morning V60");
        when(recipeService.listRecipes(eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/recipe/getRecipes")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].trackId").value(TRACK_ID_1.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Morning V60"));
    }

    @Test
    void createRecipe_returns201() throws Exception {
        RecipeVersionResponseDTO response = versionResponse("New Recipe");
        when(recipeService.createRecipe(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/recipe/createRecipe")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                        .contentType("application/json")
                        .content("""
                                {
                                  "beanId": "44444444-4444-4444-4444-444444444444",
                                  "methodId": "33333333-3333-3333-3333-333333333333",
                                  "title": "New Recipe",
                                  "global": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackId").value(TRACK_ID_1.toString()))
                .andExpect(jsonPath("$.title").value("New Recipe"));
    }

    @Test
    void updateRecipe_returns200() throws Exception {
        RecipeVersionResponseDTO response = versionResponse("Updated Recipe");
        when(recipeService.updateRecipe(eq(USER_ID), eq(TRACK_ID_1), any())).thenReturn(response);

        mockMvc.perform(patch("/api/recipe/updateRecipe/" + TRACK_ID_1)
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Updated Recipe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackId").value(TRACK_ID_1.toString()))
                .andExpect(jsonPath("$.title").value("Updated Recipe"));
    }

    @Test
    void deleteRecipe_returns204() throws Exception {
        mockMvc.perform(delete("/api/recipe/deleteRecipe/" + TRACK_ID_1)
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(recipeService).deleteRecipe(USER_ID, TRACK_ID_1);
    }

    @Test
    void getRecipe_returns200() throws Exception {
        TrackDetailsResponseDTO details = new TrackDetailsResponseDTO();
        details.setTrackId(TRACK_ID_1);
        details.setTitle("Track Details");
        when(recipeService.getRecipe(USER_ID, TRACK_ID_1)).thenReturn(details);

        mockMvc.perform(get("/api/recipe/getRecipe/" + TRACK_ID_1)
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackId").value(TRACK_ID_1.toString()))
                .andExpect(jsonPath("$.title").value("Track Details"));
    }

    @Test
    void getRecipeVersions_returns200() throws Exception {
        VersionHistoryItemDTO item = new VersionHistoryItemDTO();
        item.setVersionId(VERSION_ID);
        item.setVersionNumber(2);
        when(recipeService.listRecipeVersions(USER_ID, TRACK_ID_1)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/recipe/getRecipeVersions/" + TRACK_ID_1)
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionId").value(VERSION_ID.toString()))
                .andExpect(jsonPath("$[0].versionNumber").value(2));
    }

    @Test
    void getMetadata_returns200() throws Exception {
        MethodFieldMetadataDTO field = new MethodFieldMetadataDTO();
        field.setName("filterShape");
        field.setLabel("Filter Shape");
        field.setType("string");
        field.setRequired(false);

        MethodPayloadMetadataDTO metadata = new MethodPayloadMetadataDTO();
        metadata.setMethodKey("pour_over");
        metadata.setMethodName("V60");
        metadata.setFields(List.of(field));

        when(recipeService.getMetadata(METHOD_ID)).thenReturn(metadata);

        mockMvc.perform(get("/api/recipe/methods/" + METHOD_ID + "/metadata")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methodKey").value("pour_over"))
                .andExpect(jsonPath("$.methodName").value("V60"))
                .andExpect(jsonPath("$.fields[0].name").value("filterShape"));
    }

    @Test
    void createRecipe_whenServiceThrows400_returns400() throws Exception {
        when(recipeService.createRecipe(eq(USER_ID), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid"));

        mockMvc.perform(post("/api/recipe/createRecipe")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                        .contentType("application/json")
                        .content("""
                                {
                                  "methodId": "33333333-3333-3333-3333-333333333333"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecipes_whenJwtMissing_returns401() throws Exception {
        mockMvc.perform(get("/api/recipe/getRecipes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRecipes_whenTokenSubjectInvalid_returns401() throws Exception {
        mockMvc.perform(get("/api/recipe/getRecipes")
                        .with(jwt().jwt(token -> token.subject("not-a-uuid"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMetadata_whenJwtMissing_returns401() throws Exception {
        mockMvc.perform(get("/api/recipe/methods/" + METHOD_ID + "/metadata"))
                .andExpect(status().isUnauthorized());
    }

    private RecipeVersionResponseDTO versionResponse(String title) {
        RecipeVersionResponseDTO dto = new RecipeVersionResponseDTO();
        dto.setTrackId(TRACK_ID_1);
        dto.setVersionId(VERSION_ID);
        dto.setVersionNumber(1);
        dto.setCurrent(true);
        dto.setBeanId(BEAN_ID);
        dto.setMethodId(METHOD_ID);
        dto.setTitle(title);
        dto.setGlobal(false);
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}
