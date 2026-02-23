package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.user.UpdateRequestDTO;
import com.example.coffeenotes.api.dto.user.UserReturnDTO;
import com.example.coffeenotes.config.SecurityConfig;
import com.example.coffeenotes.feature.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class UserControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getUser_returnsDto() throws Exception {
        UserReturnDTO dto = new UserReturnDTO();
        dto.setDisplayName("Patri");
        dto.setEmail("patri@coffee.com");

        when(userService.getUser(USER_ID)).thenReturn(dto);

        mockMvc.perform(get("/api/user/getUser")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Patri"))
                .andExpect(jsonPath("$.email").value("patri@coffee.com"));
    }

    @Test
    void updateUser_returnsDto() throws Exception {
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDisplayName("Patri Trimmed");

        when(userService.updateUser(eq(USER_ID), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/user/updateUser")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "  Patri Trimmed  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Patri Trimmed"));
    }

    @Test
    void updatePassword_returns204AndClearsCookie() throws Exception {
        mockMvc.perform(patch("/api/user/updatePassword")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                        .contentType("application/json")
                        .content("""
                                {
                                  "currentPassword": "Current@123",
                                  "newPassword": "NewPass@123"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(userService).updatePassword(any(), eq(USER_ID));
    }

    @Test
    void deleteUser_returns204AndClearsCookie() throws Exception {
        mockMvc.perform(delete("/api/user/deleteUser")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(userService).deleteUser(USER_ID);
    }

    @Test
    void getUser_whenJwtMissing_returns401() throws Exception {
        mockMvc.perform(get("/api/user/getUser"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUser_whenTokenSubjectInvalid_returns401() throws Exception {
        mockMvc.perform(get("/api/user/getUser")
                        .with(jwt().jwt(token -> token.subject("not-a-uuid"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePassword_whenServiceThrows400_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid"))
                .when(userService).updatePassword(any(), eq(USER_ID));

        mockMvc.perform(patch("/api/user/updatePassword")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                        .contentType("application/json")
                        .content("""
                                {
                                  "currentPassword": "Current@123",
                                  "newPassword": "NewPass@123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_whenServiceThrows404_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(userService).deleteUser(USER_ID);

        mockMvc.perform(delete("/api/user/deleteUser")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isNotFound());
    }
}
