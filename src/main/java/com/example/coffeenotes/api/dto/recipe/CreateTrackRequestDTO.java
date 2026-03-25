package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateTrackRequestDTO {
    private UUID beanId;
    private UUID methodId;
    private String title;
    private boolean isGlobal;
    private String methodPayload;
    private List<UUID> equipmentIds;
}
