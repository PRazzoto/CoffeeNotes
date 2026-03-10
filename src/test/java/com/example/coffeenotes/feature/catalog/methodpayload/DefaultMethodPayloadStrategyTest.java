package com.example.coffeenotes.feature.catalog.methodpayload;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMethodPayloadStrategyTest {

    private final DefaultMethodPayloadStrategy strategy = new DefaultMethodPayloadStrategy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void methodKey_returnsDefault() {
        assertEquals("default", strategy.methodKey());
    }

    @Test
    void validateAndNormalize_whenNull_returnsEmptyObject() {
        JsonNode out = strategy.validateAndNormalize(null);
        assertNotNull(out);
        assertTrue(out.isObject());
        assertEquals(0, out.size());
    }

    @Test
    void validateAndNormalize_whenObject_returnsSameObject() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"x\":\"y\"}");
        JsonNode out = strategy.validateAndNormalize(payload);
        assertSame(payload, out);
        assertEquals("y", out.get("x").asText());
    }

    @Test
    void validateAndNormalize_whenArray_throws400() throws Exception {
        JsonNode payload = objectMapper.readTree("[1,2]");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strategy.validateAndNormalize(payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void metadata_returnsEmptyFieldList() {
        MethodPayloadMetadataDTO out = strategy.metadata("Any");
        assertEquals("default", out.getMethodKey());
        assertEquals("Any", out.getMethodName());
        assertNotNull(out.getFields());
        assertTrue(out.getFields().isEmpty());
    }
}
