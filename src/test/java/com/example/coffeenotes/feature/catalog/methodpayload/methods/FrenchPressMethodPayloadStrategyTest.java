package com.example.coffeenotes.feature.catalog.methodpayload.methods;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class FrenchPressMethodPayloadStrategyTest {

    private final FrenchPressMethodPayloadStrategy strategy = new FrenchPressMethodPayloadStrategy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void methodKey_returnsFrenchPress() {
        assertEquals("french_press", strategy.methodKey());
    }

    @Test
    void validateAndNormalize_whenValidPayload_normalizesPressStyle() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "steepTimeSeconds": 240,
                  "pressStyle": " FULL ",
                  "stirCount": 1,
                  "plungeSeconds": 20,
                  "crustBreak": true
                }
                """);

        JsonNode out = strategy.validateAndNormalize(payload);

        assertEquals(240, out.get("steepTimeSeconds").asInt());
        assertEquals("full", out.get("pressStyle").asText());
    }

    @Test
    void validateAndNormalize_whenMissingRequiredField_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"pressStyle\":\"full\"}");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateAndNormalize_whenInvalidPressStyle_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"steepTimeSeconds": 240, "pressStyle":"medium"}
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void metadata_returnsExpectedFields() {
        MethodPayloadMetadataDTO metadata = strategy.metadata("French Press");
        assertEquals("french_press", metadata.getMethodKey());
        assertEquals(5, metadata.getFields().size());
    }
}
