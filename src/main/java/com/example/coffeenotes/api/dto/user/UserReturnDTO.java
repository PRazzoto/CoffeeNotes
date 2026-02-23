package com.example.coffeenotes.api.dto.user;

import com.example.coffeenotes.domain.catalog.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UserReturnDTO {
    private String displayName;
    private String email;

}
