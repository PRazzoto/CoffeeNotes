package com.example.coffeenotes.api.dto.grinder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrinderConversionRequestDTO {
    private String sourceGrinderId;
    private String targetGrinderId;
    private GrinderSettingDTO sourceSetting;
}

