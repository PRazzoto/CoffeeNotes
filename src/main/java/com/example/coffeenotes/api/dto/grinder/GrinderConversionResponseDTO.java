package com.example.coffeenotes.api.dto.grinder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrinderConversionResponseDTO {
    private String sourceGrinderId;
    private String targetGrinderId;
    private GrinderSettingDTO sourceSetting;
    private GrinderSettingDTO targetSetting;
    private Integer sourceFlat;
    private Integer targetFlat;
    private Double referenceFlatEstimated;
    private String confidence;
}

