package com.example.coffeenotes.feature.catalog.methodpayload.methods;

import com.example.coffeenotes.feature.catalog.methodpayload.MethodPayloadStrategy;
import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodFieldMetadataDTO;
import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Component
public class AeroPressMethodPayloadStrategy implements MethodPayloadStrategy {

    @Override
    public String methodKey() {
        return "aeropress";
    }

    @Override
    public JsonNode validateAndNormalize(JsonNode payload) {
        if(payload == null || payload.isNull()) {
            return JsonNodeFactory.instance.objectNode();
        }
        if(!payload.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "methodPayload must be a JSON object.");
        }

        ObjectNode obj = (ObjectNode) payload;
        validateRequiredPositiveInt(obj, "steepTimeSeconds");
        validateRequiredPositiveInt(obj, "pressSeconds");
        validateOptionalEnum(obj, "orientation", Set.of("standard", "inverted"));
        validateOptionalEnum(obj, "filterType", Set.of("paper", "metal", "flow_control"));
        validateOptionalNonNegativeInt(obj, "stirSeconds");
        return obj;
    }

    @Override
    public MethodPayloadMetadataDTO metadata(String methodName) {
        MethodPayloadMetadataDTO dto = new MethodPayloadMetadataDTO();
        dto.setMethodKey(methodKey());
        dto.setMethodName(methodName);
        dto.setFields(List.of(
                field("steepTimeSeconds", "Steep Time Seconds", "integer", true),
                field("pressSeconds", "Press Seconds", "integer", true),
                field("orientation", "Orientation", "string", false),
                field("filterType", "Filter Type", "string", false),
                field("stirSeconds", "Stir Seconds", "integer", false)

        ));
        return dto;
    }

    private void validateOptionalEnum(ObjectNode obj, String fieldName, Set<String> allowed) {
        JsonNode node = obj.get(fieldName);
        if(node == null || node.isNull()) {
            return;
        }
        if(!node.isTextual()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a string.");
        }

        String value = node.asText().trim().toLowerCase();
        if(!allowed.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " has invalid value.");
        }

        obj.put(fieldName, value);
    }

    private void validateRequiredPositiveInt(ObjectNode obj, String fieldName) {
        JsonNode node = obj.get(fieldName);
        if(node == null || node.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        if(!node.isInt()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a number.");
        }
        int value = node.asInt();
        if(value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be positive.");
        }
        obj.put(fieldName, value);
    }

    private void validateOptionalNonNegativeInt(ObjectNode obj, String fieldName) {
        JsonNode node = obj.get(fieldName);
        if(node == null || node.isNull()) {
            return;
        }
        if(!node.isInt()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a number.");
        }
        int value = node.asInt();
        if(value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be 0 or greater.");
        }
        obj.put(fieldName, value);
    }
    private MethodFieldMetadataDTO field(String name, String label, String type, boolean required) {
        MethodFieldMetadataDTO f = new MethodFieldMetadataDTO();
        f.setName(name);
        f.setLabel(label);
        f.setType(type);
        f.setRequired(required);
        return f;
    }
}
