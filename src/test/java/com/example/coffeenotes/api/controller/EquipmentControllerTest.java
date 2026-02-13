package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.EquipmentDTO;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.feature.catalog.service.EquipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipmentController.class)
class EquipmentControllerTest {
    private static final UUID ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ID_10 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ID_99 = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentService equipmentService;

    @Test
    void listAll_returnsDtos() throws Exception {
        when(equipmentService.listAllEquipments()).thenReturn(List.of(
                new Equipment(ID_1, "Grinder", "Burr"),
                new Equipment(ID_2, "Scale", "Precision")
        ));

        mockMvc.perform(get("/api/equipment/listAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Grinder", "Scale")))
                .andExpect(jsonPath("$[*].description", containsInAnyOrder("Burr", "Precision")))
                .andExpect(jsonPath("$[*].id").doesNotExist());
    }

    @Test
    void create_returnsDtoAnd201() throws Exception {
        Equipment saved = new Equipment(ID_10, "Kettle", "Stovetop");
        when(equipmentService.add(any())).thenReturn(saved);

        mockMvc.perform(post("/api/equipment/createEquipment")
                        .contentType("application/json")
                        .content("{\"name\":\"Kettle\",\"description\":\"Stovetop\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kettle"))
                .andExpect(jsonPath("$.description").value("Stovetop"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void edit_returnsDtoAnd200() throws Exception {
        Equipment updated = new Equipment(ID_1, "New Name", "Old Desc");
        when(equipmentService.update(eq(ID_1), any())).thenReturn(updated);

        mockMvc.perform(put("/api/equipment/editEquipment/" + ID_1)
                        .contentType("application/json")
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("Old Desc"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void edit_whenNotFound_returns404() throws Exception {
        when(equipmentService.update(eq(ID_99), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));

        mockMvc.perform(put("/api/equipment/editEquipment/" + ID_99)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_whenServiceThrows400_returns400() throws Exception {
        when(equipmentService.add(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required"));

        mockMvc.perform(post("/api/equipment/createEquipment")
                        .contentType("application/json")
                        .content("{\"description\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edit_whenServiceThrows400_returns400() throws Exception {
        when(equipmentService.update(eq(ID_1), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "name or description is required"));

        mockMvc.perform(put("/api/equipment/editEquipment/" + ID_1)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_whenNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"))
                .when(equipmentService).delete(ID_99);

        mockMvc.perform(delete("/api/equipment/deleteEquipment/" + ID_99))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/equipment/deleteEquipment/" + ID_1))
                .andExpect(status().isNoContent());
        verify(equipmentService).delete(ID_1);
    }
}
