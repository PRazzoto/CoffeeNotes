package com.example.coffeenotes.feature.catalog.methodpayload.methods;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class CleverDripperMethodPayloadStrategyTest {

    private final CleverDripperMethodPayloadStrategy strategy = new CleverDripperMethodPayloadStrategy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void methodKey_returnsCleverDripper() {
        assertEquals("clever_dripper", strategy.methodKey());
    }

    @Test
    void validateAndNormalize_whenValidPayload_returnsNormalizedObject() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "steepTimeSeconds": 180,
                  "stirAtSeconds": 45,
                  "drawdownSeconds": 60,
                  "coveredDuringSteep": true
                }
                """);

        JsonNode out = strategy.validateAndNormalize(payload);

        assertEquals(180, out.get("steepTimeSeconds").asInt());
        assertEquals(45, out.get("stirAtSeconds").asInt());
    }

    @Test
    void validateAndNormalize_whenMissingRequiredField_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"drawdownSeconds": 60}
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateAndNormalize_whenOptionalValueInvalid_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"steepTimeSeconds":180,"stirAtSeconds":0}
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void metadata_returnsExpectedFields() {
        MethodPayloadMetadataDTO metadata = strategy.metadata("Clever Dripper");
        assertEquals("clever_dripper", metadata.getMethodKey());
        assertEquals(4, metadata.getFields().size());
    }
}
