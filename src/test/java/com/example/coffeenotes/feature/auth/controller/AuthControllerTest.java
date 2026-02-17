package com.example.coffeenotes.feature.auth.controller;

import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.feature.auth.dto.AuthLoginResultDTO;
import com.example.coffeenotes.feature.auth.dto.AuthResponseDTO;
import com.example.coffeenotes.feature.auth.dto.RegisterReturnDTO;
import com.example.coffeenotes.feature.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_returns201AndBody() throws Exception {
        RegisterReturnDTO dto = new RegisterReturnDTO();
        dto.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        dto.setEmail("test@coffee.com");
        dto.setDisplayName("Test User");
        dto.setRole(Role.USER);

        when(authService.register(any())).thenReturn(dto);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "test@coffee.com",
                                  "password": "Strong@123",
                                  "displayName": "Test User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.email").value("test@coffee.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_returns200AndTokenPayload() throws Exception {
        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setAccessToken("jwt-token");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(900L);
        AuthLoginResultDTO dto = new AuthLoginResultDTO(authResponse, "refresh-token");

        when(authService.login(any())).thenReturn(dto);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "test@coffee.com",
                                  "password": "Strong@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=refresh-token")))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void refresh_returns200AndRotatedCookie() throws Exception {
        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setAccessToken("new-jwt-token");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(900L);
        AuthLoginResultDTO dto = new AuthLoginResultDTO(authResponse, "new-refresh-token");

        when(authService.refresh("old-refresh-token")).thenReturn(dto);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=new-refresh-token")));
    }

    @Test
    void refresh_whenServiceThrows401_returns401() throws Exception {
        when(authService.refresh(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_returns204AndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("refresh-token");
    }

    @Test
    void logout_withoutCookie_stillReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout(null);
    }

    @Test
    void register_whenServiceThrows400_returns400() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "x",
                                  "password": "y",
                                  "displayName": "z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_whenServiceThrows401_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "test@coffee.com",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
