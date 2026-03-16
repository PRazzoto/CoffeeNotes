package com.example.coffeenotes.api.dto.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBeanRequestDTO {
    private String name;
    @JsonProperty("global")
    private boolean isGlobal;
    private String roaster;
    private String origin;
    private String process;
    private String notes;
}
