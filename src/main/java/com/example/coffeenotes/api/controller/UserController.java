package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.user.UpdatePasswordDTO;
import com.example.coffeenotes.api.dto.user.UpdateRequestDTO;
import com.example.coffeenotes.api.dto.user.UserReturnDTO;
import com.example.coffeenotes.feature.user.service.UserService;
import com.example.coffeenotes.util.CookieUtils;
import com.example.coffeenotes.util.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    public UserController(UserService userService) {this.userService = userService;}

    @GetMapping("/getUser")
    public UserReturnDTO getUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        return userService.getUser(userId);
    }

    @PatchMapping("/updateUser")
    public UpdateRequestDTO update(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateRequestDTO dto) {
        UUID userId = JwtUtils.extractUserId(jwt);
        return userService.updateUser(userId, dto);
    }

    @PatchMapping("/updatePassword")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> updatePassword(@RequestBody UpdatePasswordDTO dto, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        userService.updatePassword(dto, userId);
        ResponseCookie clearCookie = CookieUtils.buildRefreshCookie("", 0, cookieSecure);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    @DeleteMapping("/deleteUser")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        userService.deleteUser(userId);
        ResponseCookie clearCookie = CookieUtils.buildRefreshCookie("", 0, cookieSecure);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }
}
