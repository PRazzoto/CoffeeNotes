package com.example.coffeenotes.api.dto.catalog;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserEquipmentSelectionDTO {
    private List<UUID> equipmentIds;
}
