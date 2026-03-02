package com.example.coffeenotes.api.dto.recipe;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TrackSummaryResponseDTO {
    private UUID trackId;
    private UUID beanId;
    private String beanName;
    private UUID methodId;
    private String methodName;

    private String title;
    private Integer currentVersionNumber;
    private Integer rating;

    private boolean isGlobal;
    private boolean favorite;

    private LocalDateTime updatedAt;
}
