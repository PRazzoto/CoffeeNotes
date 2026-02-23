package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.user.UpdatePasswordDTO;
import com.example.coffeenotes.api.dto.user.UpdateRequestDTO;
import com.example.coffeenotes.api.dto.user.UserReturnDTO;
import com.example.coffeenotes.feature.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private static final long REFRESH_COOKIE_MAX_AGE_DAYS = 14;
    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    public UserController(UserService userService) {this.userService = userService;}

    @GetMapping("/getUser")
    public UserReturnDTO getUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserId(jwt);
        return userService.getUser(userId);
    }

    @PatchMapping("/updateUser")
    public UpdateRequestDTO update(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateRequestDTO dto) {
        UUID userId = getUserId(jwt);
        return userService.updateUser(userId, dto);
    }

    @PatchMapping("/updatePassword")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> updatePassword(@RequestBody UpdatePasswordDTO dto, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserId(jwt);
        userService.updatePassword(dto, userId);
        ResponseCookie clearCookie = buildRefreshCookie("", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    @DeleteMapping("/deleteUser")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserId(jwt);
        userService.deleteUser(userId);
        ResponseCookie clearCookie = buildRefreshCookie("", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }



    private UUID getUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token.");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token.");
        }
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
