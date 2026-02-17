package com.example.coffeenotes.feature.auth.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AuthResponseDTO {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
}
