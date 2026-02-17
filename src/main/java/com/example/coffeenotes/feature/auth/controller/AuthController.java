package com.example.coffeenotes.feature.auth.controller;

import com.example.coffeenotes.feature.auth.dto.*;
import com.example.coffeenotes.feature.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final long REFRESH_COOKIE_MAX_AGE_DAYS = 14;

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
        ResponseCookie refreshCookie = buildRefreshCookie(dto.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(dto.getAuthResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        AuthLoginResultDTO request = authService.refresh(refreshToken);
        ResponseCookie refreshCookie = buildRefreshCookie(request.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(request.getAuthResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie clearCookie = buildRefreshCookie("", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    private ResponseCookie buildRefreshCookie(String value) {
        return buildRefreshCookie(value, REFRESH_COOKIE_MAX_AGE_DAYS);
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeDays) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(maxAgeDays))
                .build();
    }
}
