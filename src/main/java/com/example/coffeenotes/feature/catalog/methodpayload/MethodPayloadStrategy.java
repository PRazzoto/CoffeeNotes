package com.example.coffeenotes.feature.catalog.methodpayload;

import com.example.coffeenotes.feature.catalog.methodpayload.dto.MethodPayloadMetadataDTO;
import com.fasterxml.jackson.databind.JsonNode;

public interface MethodPayloadStrategy {
    String methodKey();
    JsonNode validateAndNormalize(JsonNode payload);
    MethodPayloadMetadataDTO metadata(String methodName);
}
