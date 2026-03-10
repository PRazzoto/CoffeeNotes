package com.example.coffeenotes.feature.catalog.methodpayload.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MethodPayloadMetadataDTO {
    private String methodKey;
    private String methodName;
    private List<MethodFieldMetadataDTO> fields;
}
