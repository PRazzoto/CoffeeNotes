package com.example.coffeenotes.feature.catalog.methodpayload.methods;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class PourOverMethodPayloadStrategyTest {

    private final PourOverMethodPayloadStrategy strategy = new PourOverMethodPayloadStrategy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void methodKey_returnsPourOver() {
        assertEquals("pour_over", strategy.methodKey());
    }

    @Test
    void validateAndNormalize_whenValidEnums_normalizesCaseAndTrims() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "filterShape": " Cone ",
                  "pourStyle": "PULSE",
                  "agitation": " SwIrL ",
                  "dripperModel": "  Origami Air  "
                }
                """);

        JsonNode out = strategy.validateAndNormalize(payload);

        assertEquals("cone", out.get("filterShape").asText());
        assertEquals("pulse", out.get("pourStyle").asText());
        assertEquals("swirl", out.get("agitation").asText());
        assertEquals("Origami Air", out.get("dripperModel").asText());
    }

    @Test
    void validateAndNormalize_whenDripperModelBlank_removesField() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                { "dripperModel": "   " }
                """);

        JsonNode out = strategy.validateAndNormalize(payload);

        assertTrue(out.isObject());
        assertNull(out.get("dripperModel"));
    }

    @Test
    void validateAndNormalize_whenFilterShapeInvalid_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                { "filterShape": "basket" }
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateAndNormalize_whenPourStyleWrongType_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                { "pourStyle": 123 }
                """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateAndNormalize_whenPayloadIsArray_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("[1]");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void metadata_returnsAllFourFields() {
        MethodPayloadMetadataDTO metadata = strategy.metadata("V60");
        assertEquals("pour_over", metadata.getMethodKey());
        assertEquals("V60", metadata.getMethodName());
        assertNotNull(metadata.getFields());
        assertEquals(4, metadata.getFields().size());
    }
}
