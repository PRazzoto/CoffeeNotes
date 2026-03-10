package com.example.coffeenotes.feature.catalog.methodpayload.methods;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class MokaPotMethodPayloadStrategyTest {

    private final MokaPotMethodPayloadStrategy strategy = new MokaPotMethodPayloadStrategy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void methodKey_returnsMokaPot() {
        assertEquals("moka_pot", strategy.methodKey());
    }

    @Test
    void validateAndNormalize_whenValidPayload_normalizesHeatLevel() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "heatLevel": " HIGH ",
                  "removeOnGurgle": true,
                  "preheatedWater": false,
                  "yieldMl": 120
                }
                """);

        JsonNode out = strategy.validateAndNormalize(payload);

        assertEquals("high", out.get("heatLevel").asText());
        assertEquals(120, out.get("yieldMl").asInt());
    }

    @Test
    void validateAndNormalize_whenMissingRequiredHeatLevel_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"yieldMl": 120}
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateAndNormalize_whenYieldNotPositive_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"heatLevel":"low","yieldMl":0}
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void metadata_returnsExpectedFields() {
        MethodPayloadMetadataDTO metadata = strategy.metadata("Moka Pot");
        assertEquals("moka_pot", metadata.getMethodKey());
        assertEquals(4, metadata.getFields().size());
    }
}
