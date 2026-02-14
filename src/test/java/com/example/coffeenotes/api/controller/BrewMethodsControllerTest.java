package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.feature.catalog.service.BrewMethodsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrewMethodsController.class)
class BrewMethodsControllerTest {
    private static final UUID ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ID_10 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ID_99 = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrewMethodsService brewMethodsService;

    @Test
    void listAll_returnsDtos() throws Exception {
        when(brewMethodsService.listAllBrewMethods()).thenReturn(List.of(
                new BrewMethods(ID_1, "V60", "Cone dripper"),
                new BrewMethods(ID_2, "French Press", "Immersion")
        ));

        mockMvc.perform(get("/api/brewMethods/listAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("V60", "French Press")))
                .andExpect(jsonPath("$[*].description", containsInAnyOrder("Cone dripper", "Immersion")))
                .andExpect(jsonPath("$[*].id").doesNotExist());
    }

    @Test
    void create_returnsDtoAnd201() throws Exception {
        BrewMethods saved = new BrewMethods(ID_10, "AeroPress", "Immersion");
        when(brewMethodsService.add(any())).thenReturn(saved);

        mockMvc.perform(post("/api/brewMethods/createBrewMethods")
                        .contentType("application/json")
                        .content("{\"name\":\"AeroPress\",\"description\":\"Immersion\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("AeroPress"))
                .andExpect(jsonPath("$.description").value("Immersion"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void edit_returnsDtoAnd200() throws Exception {
        BrewMethods updated = new BrewMethods(ID_1, "Kalita Wave", "Flat-bottom dripper");
        when(brewMethodsService.update(eq(ID_1), any())).thenReturn(updated);

        mockMvc.perform(put("/api/brewMethods/editBrewMethods/" + ID_1)
                        .contentType("application/json")
                        .content("{\"name\":\"Kalita Wave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kalita Wave"))
                .andExpect(jsonPath("$.description").value("Flat-bottom dripper"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void edit_whenNotFound_returns404() throws Exception {
        when(brewMethodsService.update(eq(ID_99), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "BrewMethods not found"));

        mockMvc.perform(put("/api/brewMethods/editBrewMethods/" + ID_99)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_whenServiceThrows400_returns400() throws Exception {
        when(brewMethodsService.add(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required"));

        mockMvc.perform(post("/api/brewMethods/createBrewMethods")
                        .contentType("application/json")
                        .content("{\"description\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edit_whenServiceThrows400_returns400() throws Exception {
        when(brewMethodsService.update(eq(ID_1), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "name or description is required"));

        mockMvc.perform(put("/api/brewMethods/editBrewMethods/" + ID_1)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_whenNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "BrewMethods not found"))
                .when(brewMethodsService).delete(ID_99);

        mockMvc.perform(delete("/api/brewMethods/deleteBrewMethods/" + ID_99))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/brewMethods/deleteBrewMethods/" + ID_1))
                .andExpect(status().isNoContent());
        verify(brewMethodsService).delete(ID_1);
    }
}
