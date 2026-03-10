package com.example.coffeenotes.feature.catalog.methodpayload;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MethodPayloadStrategyRegistry{

    private final Map<String, MethodPayloadStrategy> strategiesByKey;

    public MethodPayloadStrategyRegistry(List<MethodPayloadStrategy> strategies) {
        Map<String, MethodPayloadStrategy> map = new LinkedHashMap<>();

        for(MethodPayloadStrategy strategy : strategies){
            String key = normalize(strategy.methodKey());
            map.put(key, strategy);
        }
        this.strategiesByKey = Map.copyOf(map);
    }

    public MethodPayloadStrategy getRequired(String methodKey) {
        if (methodKey == null || methodKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brew method name is required");
        }

        String normalized = normalize(methodKey);

        String resolvedKey;
        if (normalized.equals("v60") || normalized.equals("chemex") || normalized.equals("melitta_pour_over") || normalized.equals("kalita_wave") || normalized.equals("origami_dripper")) {
            resolvedKey = "pour_over";
        } else {
            resolvedKey = normalized;
        }

        MethodPayloadStrategy strategy = strategiesByKey.get(resolvedKey);
        if (strategy == null) {
            MethodPayloadStrategy fallbackStrategy = strategiesByKey.get("pour_over");
            if (fallbackStrategy == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No 'default' fallback method payload strategy configured.");
            }
            strategy = fallbackStrategy;
        }
        return strategy;
    }

    public MethodPayloadMetadataDTO metadata(String methodKey, String methodName) {
        return getRequired(methodKey).metadata(methodName);
    }

    private String normalize(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }

}
