package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class VersionHistoryItemDTO {
    private UUID versionId;
    private Integer versionNumber;
    private boolean isCurrent;
    private String title;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
