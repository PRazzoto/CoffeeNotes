package com.example.coffeenotes.api.dto.user;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UpdatePasswordDTO {
    private String currentPassword;
    private String newPassword;
}
