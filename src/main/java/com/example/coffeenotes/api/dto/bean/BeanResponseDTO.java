package com.example.coffeenotes.api.dto.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BeanResponseDTO {
    private UUID id;
    private String name;
    private boolean isGlobal;
    private String roaster;
    private String origin;
    private String process;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
