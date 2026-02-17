package com.example.coffeenotes.config;

import com.example.coffeenotes.api.controller.EquipmentController;
import com.example.coffeenotes.feature.auth.controller.AuthController;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.feature.auth.dto.AuthLoginResultDTO;
import com.example.coffeenotes.feature.auth.dto.AuthResponseDTO;
import com.example.coffeenotes.feature.auth.dto.RegisterReturnDTO;
import com.example.coffeenotes.feature.auth.service.AuthService;
import com.example.coffeenotes.feature.catalog.service.EquipmentService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EquipmentController.class, AuthController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentService equipmentService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/equipment/listAll"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withJwt_returns200() throws Exception {
        when(equipmentService.listAllEquipments()).thenReturn(List.of(new Equipment()));

        mockMvc.perform(get("/api/equipment/listAll")
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void authLoginEndpoint_withoutToken_isPublic() throws Exception {
        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setAccessToken("token");
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
                .andExpect(status().isOk());
    }

    @Test
    void authRegisterEndpoint_withoutToken_isPublic() throws Exception {
        RegisterReturnDTO dto = new RegisterReturnDTO();
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
                .andExpect(status().isCreated());
    }

    @Test
    void authRefreshEndpoint_withoutToken_isPublic() throws Exception {
        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setAccessToken("new-token");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(900L);
        AuthLoginResultDTO dto = new AuthLoginResultDTO(authResponse, "new-refresh");

        when(authService.refresh(any())).thenReturn(dto);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk());
    }

    @Test
    void authLogoutEndpoint_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authLogoutEndpoint_withJwt_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refresh_token", "refresh-token"))
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }
}
