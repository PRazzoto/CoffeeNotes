package com.example.coffeenotes.api.dto.grinder;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrinderCatalogItemDTO {
    private String id;
    private String name;
    private String make;
    private String model;
    private String tier;
    private List<GrinderUnitDTO> units;
}

