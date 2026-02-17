package com.example.coffeenotes.feature.auth.controller;

import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.feature.auth.dto.AuthResponseDTO;
import com.example.coffeenotes.feature.auth.dto.RegisterReturnDTO;
import com.example.coffeenotes.feature.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        AuthResponseDTO dto = new AuthResponseDTO();
        dto.setAccessToken("jwt-token");
        dto.setTokenType("Bearer");
        dto.setExpiresIn(900L);

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
                .andExpect(jsonPath("$.expiresIn").value(900));
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

