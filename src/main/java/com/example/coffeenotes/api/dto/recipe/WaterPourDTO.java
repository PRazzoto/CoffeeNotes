package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WaterPourDTO {
    private Integer waterAmountMl;
    private String time;
    private Integer orderIndex;
}
