package com.example.coffeenotes.api.dto.catalog;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BrewMethodsDTO {
    private UUID id;
    private String name;
    private String description;
}
