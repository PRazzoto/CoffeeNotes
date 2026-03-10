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
public class CleverDripperMethodPayloadStrategy implements MethodPayloadStrategy {

    @Override
    public String methodKey() {
        return "clever_dripper";
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
        validateOptionalPositiveInt(obj, "stirAtSeconds");
        validateOptionalPositiveInt(obj, "drawdownSeconds");
        validateOptionalBoolean(obj, "coveredDuringSteep");

        return obj;
    }

    @Override
    public MethodPayloadMetadataDTO metadata(String methodName) {
        MethodPayloadMetadataDTO dto = new MethodPayloadMetadataDTO();
        dto.setMethodKey(methodKey());
        dto.setMethodName(methodName);
        dto.setFields(List.of(
                field("steepTimeSeconds", "Steep Time Seconds", "integer", true),
                field("stirAtSeconds", "Stir At Seconds", "integer", false),
                field("drawdownSeconds", "Drawdown Seconds", "integer", false),
                field("coveredDuringSteep", "Covered During Steep", "boolean", false)
        ));
        return dto;
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

    private void validateOptionalPositiveInt(ObjectNode obj, String fieldName) {
        JsonNode node = obj.get(fieldName);
        if(node == null || node.isNull()) {
            return;
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

    private void validateOptionalBoolean(ObjectNode obj, String fieldName) {
        JsonNode node = obj.get(fieldName);
        if(node == null || node.isNull()) {
            return;
        }
        if(!node.isBoolean()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a boolean.");
        }
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
