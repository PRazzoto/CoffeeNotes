package com.example.coffeenotes.feature.auth.controller;

import com.example.coffeenotes.feature.auth.dto.AuthResponseDTO;
import com.example.coffeenotes.feature.auth.dto.LoginRequestDTO;
import com.example.coffeenotes.feature.auth.dto.RegisterRequestDTO;
import com.example.coffeenotes.feature.auth.dto.RegisterReturnDTO;
import com.example.coffeenotes.feature.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterReturnDTO> register(@RequestBody RegisterRequestDTO registerRequestDTO) {
        RegisterReturnDTO dto = authService.register(registerRequestDTO);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO login) {
        AuthResponseDTO dto = authService.login(login);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}
