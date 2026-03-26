package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.grinder.GrinderCatalogItemDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderConversionResponseDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderSettingDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderUnitDTO;
import com.example.coffeenotes.config.SecurityConfig;
import com.example.coffeenotes.feature.catalog.service.GrinderConversionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GrinderConversionController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class GrinderConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrinderConversionService grinderConversionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listSupportedGrinders_returns200AndBody() throws Exception {
        GrinderUnitDTO unit = new GrinderUnitDTO();
        unit.setLabel("click");
        unit.setMaximum(40);

        GrinderCatalogItemDTO item = new GrinderCatalogItemDTO();
        item.setId("baratza_encore");
        item.setName("Baratza Encore");
        item.setMake("Baratza");
        item.setModel("Encore");
        item.setTier("free");
        item.setUnits(List.of(unit));

        when(grinderConversionService.listSupportedGrinders()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/grinder-conversion/grinders")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("baratza_encore"))
                .andExpect(jsonPath("$[0].units[0].label").value("click"))
                .andExpect(jsonPath("$[0].units[0].maximum").value(40));
    }

    @Test
    void convert_returns200AndConvertedSetting() throws Exception {
        GrinderSettingDTO source = new GrinderSettingDTO();
        source.setClick(20);

        GrinderSettingDTO target = new GrinderSettingDTO();
        target.setClick(24);

        GrinderConversionResponseDTO response = new GrinderConversionResponseDTO();
        response.setSourceGrinderId("baratza_encore");
        response.setTargetGrinderId("comandante_c40");
        response.setSourceSetting(source);
        response.setTargetSetting(target);
        response.setSourceFlat(20);
        response.setTargetFlat(24);
        response.setReferenceFlatEstimated(24.0);
        response.setConfidence("high");

        when(grinderConversionService.convert(org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(post("/api/grinder-conversion/convert")
                        .with(jwt())
                        .contentType("application/json")
                        .content("""
                                {
                                  "sourceGrinderId": "baratza_encore",
                                  "targetGrinderId": "comandante_c40",
                                  "sourceSetting": {
                                    "click": 20
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceGrinderId").value("baratza_encore"))
                .andExpect(jsonPath("$.targetGrinderId").value("comandante_c40"))
                .andExpect(jsonPath("$.targetSetting.click").value(24))
                .andExpect(jsonPath("$.confidence").value("high"));
    }

    @Test
    void convert_whenServiceThrows404_returnsStandardErrorBody() throws Exception {
        when(grinderConversionService.convert(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Grinder not found."));

        mockMvc.perform(post("/api/grinder-conversion/convert")
                        .with(jwt())
                        .contentType("application/json")
                        .content("""
                                {
                                  "sourceGrinderId": "x",
                                  "targetGrinderId": "y"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Grinder not found."))
                .andExpect(jsonPath("$.path").value("/api/grinder-conversion/convert"));
    }

    @Test
    void listSupportedGrinders_whenJwtMissing_returns401() throws Exception {
        mockMvc.perform(get("/api/grinder-conversion/grinders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void convert_whenJwtMissing_returns401() throws Exception {
        mockMvc.perform(post("/api/grinder-conversion/convert")
                        .contentType("application/json")
                        .content("""
                                {
                                  "sourceGrinderId": "baratza_encore",
                                  "targetGrinderId": "comandante_c40",
                                  "sourceSetting": {
                                    "click": 20
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
