package com.example.coffeenotes.feature.auth.controller;

import com.example.coffeenotes.feature.auth.dto.*;
import com.example.coffeenotes.feature.auth.service.AuthService;
import com.example.coffeenotes.util.CookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

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
        AuthLoginResultDTO dto = authService.login(login);
        ResponseCookie refreshCookie = CookieUtils.buildRefreshCookie(dto.getRefreshToken(), cookieSecure);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(dto.getAuthResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        AuthLoginResultDTO request = authService.refresh(refreshToken);
        ResponseCookie refreshCookie = CookieUtils.buildRefreshCookie(request.getRefreshToken(), cookieSecure);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(request.getAuthResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie clearCookie = CookieUtils.buildRefreshCookie("", 0, cookieSecure);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }
}
