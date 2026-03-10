package com.example.coffeenotes.feature.catalog.methodpayload;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.example.coffeenotes.feature.catalog.methodpayload.methods.PourOverMethodPayloadStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodPayloadStrategyRegistryTest {

    private final MethodPayloadStrategyRegistry registry = new MethodPayloadStrategyRegistry(
            List.of(new DefaultMethodPayloadStrategy(), new PourOverMethodPayloadStrategy())
    );

    @Test
    void getRequired_whenV60Alias_returnsPourOverStrategy() {
        MethodPayloadStrategy strategy = registry.getRequired("V60");
        assertEquals("pour_over", strategy.methodKey());
    }

    @Test
    void getRequired_whenChemexAlias_returnsPourOverStrategy() {
        MethodPayloadStrategy strategy = registry.getRequired("chemex");
        assertEquals("pour_over", strategy.methodKey());
    }

    @Test
    void getRequired_whenUnsupportedMethod_throws400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> registry.getRequired("unsupported_method"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void metadata_whenAliasMethod_returnsPourOverMetadata() {
        MethodPayloadMetadataDTO metadata = registry.metadata("origami_dripper", "Origami");
        assertEquals("pour_over", metadata.getMethodKey());
        assertEquals("Origami", metadata.getMethodName());
        assertEquals(4, metadata.getFields().size());
    }
}
