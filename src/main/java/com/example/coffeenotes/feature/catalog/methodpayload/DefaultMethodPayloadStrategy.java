package com.example.coffeenotes.feature.catalog.methodpayload;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.List;

@Component
public class DefaultMethodPayloadStrategy implements MethodPayloadStrategy{
    @Override
    public String methodKey(){
        return "default";
    }

    @Override
    public JsonNode validateAndNormalize(JsonNode payload){
        if(payload == null || payload.isNull()) {
            return JsonNodeFactory.instance.objectNode();
        }
        if(!payload.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "methodPayload must be a JSON object.");
        }
        return payload;
    }

    @Override
    public MethodPayloadMetadataDTO metadata(String methodName) {
        MethodPayloadMetadataDTO dto = new MethodPayloadMetadataDTO();
        dto.setMethodName(methodName);
        dto.setMethodKey(methodKey());
        dto.setFields(List.of());
        return dto;
    }
}
