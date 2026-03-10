package com.example.coffeenotes.feature.catalog.methodpayload.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MethodFieldMetadataDTO {
    private String name;
    private String label;
    private String type;
    private boolean required;
}
