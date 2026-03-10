package com.example.coffeenotes.feature.catalog.methodpayload;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.example.coffeenotes.feature.catalog.methodpayload.methods.AeroPressMethodPayloadStrategy;
import com.example.coffeenotes.feature.catalog.methodpayload.methods.CleverDripperMethodPayloadStrategy;
import com.example.coffeenotes.feature.catalog.methodpayload.methods.FrenchPressMethodPayloadStrategy;
import com.example.coffeenotes.feature.catalog.methodpayload.methods.MokaPotMethodPayloadStrategy;
import com.example.coffeenotes.feature.catalog.methodpayload.methods.PourOverMethodPayloadStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodPayloadStrategyRegistryTest {

    private final MethodPayloadStrategyRegistry registry = new MethodPayloadStrategyRegistry(
            List.of(
                    new DefaultMethodPayloadStrategy(),
                    new PourOverMethodPayloadStrategy(),
                    new FrenchPressMethodPayloadStrategy(),
                    new AeroPressMethodPayloadStrategy(),
                    new MokaPotMethodPayloadStrategy(),
                    new CleverDripperMethodPayloadStrategy()
            )
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
    void getRequired_whenUnsupportedMethod_returnsDefaultFallback() {
        MethodPayloadStrategy strategy = registry.getRequired("unsupported_method");
        assertEquals("default", strategy.methodKey());
    }

    @Test
    void getRequired_whenMethodHasSpaces_resolvesToUnderscoreKey() {
        MethodPayloadStrategy strategy = registry.getRequired("French Press");
        assertEquals("french_press", strategy.methodKey());
    }

    @Test
    void getRequired_whenMethodHasHyphen_resolvesToUnderscoreKey() {
        MethodPayloadStrategy strategy = registry.getRequired("moka-pot");
        assertEquals("moka_pot", strategy.methodKey());
    }

    @Test
    void metadata_whenAliasMethod_returnsPourOverMetadata() {
        MethodPayloadMetadataDTO metadata = registry.metadata("origami_dripper", "Origami");
        assertEquals("pour_over", metadata.getMethodKey());
        assertEquals("Origami", metadata.getMethodName());
        assertEquals(4, metadata.getFields().size());
    }
}
