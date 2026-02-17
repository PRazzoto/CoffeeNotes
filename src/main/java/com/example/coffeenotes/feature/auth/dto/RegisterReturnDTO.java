package com.example.coffeenotes.feature.auth.dto;

import com.example.coffeenotes.domain.catalog.Role;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RegisterReturnDTO {
    UUID id;
    String email;
    String displayName;
    Role role;
}
