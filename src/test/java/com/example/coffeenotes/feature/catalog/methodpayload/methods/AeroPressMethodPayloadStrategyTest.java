package com.example.coffeenotes.feature.catalog.methodpayload.methods;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AeroPressMethodPayloadStrategyTest {

    private final AeroPressMethodPayloadStrategy strategy = new AeroPressMethodPayloadStrategy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void methodKey_returnsAeroPress() {
        assertEquals("aeropress", strategy.methodKey());
    }

    @Test
    void validateAndNormalize_whenValidPayload_normalizesEnums() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "steepTimeSeconds": 60,
                  "pressSeconds": 30,
                  "orientation": " INVERTED ",
                  "filterType": " METAL ",
                  "stirSeconds": 10
                }
                """);

        JsonNode out = strategy.validateAndNormalize(payload);

        assertEquals("inverted", out.get("orientation").asText());
        assertEquals("metal", out.get("filterType").asText());
    }

    @Test
    void validateAndNormalize_whenMissingRequiredField_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"steepTimeSeconds": 60}
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateAndNormalize_whenInvalidEnum_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"steepTimeSeconds": 60, "pressSeconds": 30, "orientation":"sideways"}
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void metadata_returnsExpectedFields() {
        MethodPayloadMetadataDTO metadata = strategy.metadata("AeroPress");
        assertEquals("aeropress", metadata.getMethodKey());
        assertEquals(5, metadata.getFields().size());
    }
}
