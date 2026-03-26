package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.grinder.GrinderCatalogItemDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderConversionRequestDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderConversionResponseDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderSettingDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GrinderConversionServiceTest {
    private GrinderConversionService grinderConversionService;

    @BeforeEach
    void setUp() {
        grinderConversionService = new GrinderConversionService(new ObjectMapper());
    }

    @Test
    void listSupportedGrinders_returnsKnownItemsWithUnits() {
        List<GrinderCatalogItemDTO> grinders = grinderConversionService.listSupportedGrinders();

        assertFalse(grinders.isEmpty());
        Optional<GrinderCatalogItemDTO> baratza = grinders.stream()
                .filter(g -> "baratza_encore".equals(g.getId()))
                .findFirst();

        assertTrue(baratza.isPresent());
        assertEquals("Baratza Encore", baratza.get().getName());
        assertEquals(1, baratza.get().getUnits().size());
        assertEquals("click", baratza.get().getUnits().get(0).getLabel());
    }

    @Test
    void convert_baratzaEncoreToComandante_returnsExpectedTargetSetting() {
        GrinderConversionRequestDTO request = new GrinderConversionRequestDTO();
        request.setSourceGrinderId("baratza_encore");
        request.setTargetGrinderId("comandante_c40");

        GrinderSettingDTO sourceSetting = new GrinderSettingDTO();
        sourceSetting.setClick(20);
        request.setSourceSetting(sourceSetting);

        GrinderConversionResponseDTO response = grinderConversionService.convert(request);

        assertEquals("baratza_encore", response.getSourceGrinderId());
        assertEquals("comandante_c40", response.getTargetGrinderId());
        assertNotNull(response.getTargetSetting());
        assertEquals(24, response.getTargetSetting().getClick());
        assertEquals(20, response.getSourceFlat());
        assertEquals(24, response.getTargetFlat());
        assertEquals("high", response.getConfidence());
    }

    @Test
    void convert_whenSameGrinder_clampsOutOfRangeInput() {
        GrinderConversionRequestDTO request = new GrinderConversionRequestDTO();
        request.setSourceGrinderId("baratza_encore");
        request.setTargetGrinderId("baratza_encore");

        GrinderSettingDTO sourceSetting = new GrinderSettingDTO();
        sourceSetting.setClick(500);
        request.setSourceSetting(sourceSetting);

        GrinderConversionResponseDTO response = grinderConversionService.convert(request);

        assertEquals(40, response.getSourceSetting().getClick());
        assertEquals(40, response.getTargetSetting().getClick());
        assertEquals(40, response.getSourceFlat());
        assertEquals(40, response.getTargetFlat());
    }

    @Test
    void convert_whenRequestMissingFields_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grinderConversionService.convert(new GrinderConversionRequestDTO())
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void convert_whenGrinderNotFound_throws404() {
        GrinderConversionRequestDTO request = new GrinderConversionRequestDTO();
        request.setSourceGrinderId("unknown_grinder");
        request.setTargetGrinderId("baratza_encore");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grinderConversionService.convert(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}

