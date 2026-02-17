package com.example.coffeenotes.feature.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthLoginResultDTO {
    private AuthResponseDTO authResponse;
    private String refreshToken;

    public AuthLoginResultDTO(AuthResponseDTO response, String refreshToken) {
        this.authResponse = response;
        this.refreshToken = refreshToken;
    }
}
