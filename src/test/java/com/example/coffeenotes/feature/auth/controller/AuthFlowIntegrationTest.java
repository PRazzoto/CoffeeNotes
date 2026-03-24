package com.example.coffeenotes.feature.auth.controller;

import com.example.coffeenotes.domain.auth.AuthRefreshSession;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.auth.repository.AuthRefreshSessionRepository;
import com.example.coffeenotes.feature.auth.service.RefreshTokenService;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthRefreshSessionRepository authRefreshSessionRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanupUsers() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (String email : createdEmails) {
                userRepository.findByEmail(email).ifPresent(user -> {
                    authRefreshSessionRepository.deleteByUser_Id(user.getId());
                    userRepository.delete(user);
                });
            }
            createdEmails.clear();
        });
    }

    @Test
    void register_login_refresh_logout_flow_works_endToEnd() throws Exception {
        String rawEmail = "Flow." + UUID.randomUUID() + "@Coffee.test";
        String normalizedEmail = rawEmail.toLowerCase();
        String password = "Strong@123";
        String displayName = "  Flow User  ";
        createdEmails.add(normalizedEmail);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(rawEmail, password, displayName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(normalizedEmail))
                .andExpect(jsonPath("$.displayName").value("Flow User"))
                .andExpect(jsonPath("$.role").value("USER"));

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(rawEmail, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();
        String refreshToken = extractCookieValue(loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE), "refresh_token");

        Jwt jwt = jwtDecoder.decode(accessToken);
        assertEquals(user.getId().toString(), jwt.getSubject());
        assertEquals(normalizedEmail, jwt.getClaimAsString("email"));
        assertEquals("USER", jwt.getClaimAsString("role"));
        assertTrue(jwt.getAudience().contains("coffeenotes-web"));

        List<AuthRefreshSession> activeSessions = authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId());
        assertEquals(1, activeSessions.size());
        assertEquals(refreshTokenService.hashToken(refreshToken), activeSessions.get(0).getTokenHash());
        assertNull(activeSessions.get(0).getRevokedAt());

        mockMvc.perform(get("/api/user/getUser")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(normalizedEmail))
                .andExpect(jsonPath("$.displayName").value("Flow User"));

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String refreshedAccessToken = refreshBody.get("accessToken").asText();
        String rotatedRefreshToken = extractCookieValue(refreshResult.getResponse().getHeader(HttpHeaders.SET_COOKIE), "refresh_token");

        assertNotEquals(refreshToken, rotatedRefreshToken);
        Jwt refreshedJwt = jwtDecoder.decode(refreshedAccessToken);
        assertEquals(user.getId().toString(), refreshedJwt.getSubject());
        assertEquals(normalizedEmail, refreshedJwt.getClaimAsString("email"));
        assertEquals("USER", refreshedJwt.getClaimAsString("role"));

        List<AuthRefreshSession> allSessions = authRefreshSessionRepository.findAll().stream()
                .filter(session -> session.getUser().getId().equals(user.getId()))
                .toList();
        assertEquals(2, allSessions.size());
        assertEquals(1, allSessions.stream().filter(session -> session.getRevokedAt() == null).count());
        assertEquals(1, allSessions.stream().filter(session -> session.getRevokedAt() != null).count());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid Refresh Token."));

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refresh_token", rotatedRefreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertTrue(authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).isEmpty());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", rotatedRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid Refresh Token."));
    }

    @Test
    void register_whenEmailAlreadyExistsIgnoringCase_returns409AndKeepsSingleUser() throws Exception {
        String firstEmail = ("Duplicate." + UUID.randomUUID() + "@Coffee.test").toLowerCase();
        String secondEmail = firstEmail.toUpperCase();
        createdEmails.add(firstEmail);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Strong@123",
                                  "displayName": "First User"
                                }
                                """.formatted(firstEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(firstEmail));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Strong@123",
                                  "displayName": "Second User"
                                }
                                """.formatted(secondEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already registered."))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));

        long matchingUsers = userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals(firstEmail))
                .count();
        assertEquals(1, matchingUsers);
    }

    @Test
    void updatePassword_revokesAllRefreshSessions_and_requiresNewPasswordForFutureLogin() throws Exception {
        String email = ("password-flow." + UUID.randomUUID() + "@coffee.test").toLowerCase();
        String oldPassword = "Strong@123";
        String newPassword = "EvenStronger@456";
        createdEmails.add(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Password Flow"
                                }
                                """.formatted(email, oldPassword)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();

        MvcResult firstLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, oldPassword)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult secondLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, oldPassword)))
                .andExpect(status().isOk())
                .andReturn();

        String firstAccessToken = objectMapper.readTree(firstLogin.getResponse().getContentAsString()).get("accessToken").asText();
        String firstRefreshToken = extractCookieValue(firstLogin.getResponse().getHeader(HttpHeaders.SET_COOKIE), "refresh_token");
        String secondRefreshToken = extractCookieValue(secondLogin.getResponse().getHeader(HttpHeaders.SET_COOKIE), "refresh_token");

        assertEquals(2, authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).size());

        mockMvc.perform(patch("/api/user/updatePassword")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstAccessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s"
                                }
                                """.formatted(oldPassword, newPassword)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertTrue(authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).isEmpty());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Refresh Token."));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", secondRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Refresh Token."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, oldPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Credentials."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        assertEquals(1, authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).size());
    }

    @Test
    void deleteUser_removesAccountClearsSessionsAndBlocksFutureAuthUse() throws Exception {
        String email = ("delete-flow." + UUID.randomUUID() + "@coffee.test").toLowerCase();
        String password = "Strong@123";
        createdEmails.add(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Delete Flow"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();

        MvcResult firstLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult secondLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(firstLogin.getResponse().getContentAsString()).get("accessToken").asText();
        String firstRefreshToken = extractCookieValue(firstLogin.getResponse().getHeader(HttpHeaders.SET_COOKIE), "refresh_token");
        String secondRefreshToken = extractCookieValue(secondLogin.getResponse().getHeader(HttpHeaders.SET_COOKIE), "refresh_token");

        assertEquals(2, authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).size());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", firstRefreshToken)))
                .andExpect(status().isOk());

        assertEquals(2, authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).size());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/user/deleteUser")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertTrue(userRepository.findByEmail(email).isEmpty());
        long remainingSessions = authRefreshSessionRepository.findAll().stream()
                .filter(session -> session.getUser().getId().equals(user.getId()))
                .count();
        assertEquals(0, remainingSessions);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Refresh Token."));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", secondRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Refresh Token."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Credentials."));

        mockMvc.perform(get("/api/user/getUser")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    private String extractCookieValue(String setCookieHeader, String cookieName) {
        assertNotNull(setCookieHeader);
        for (String part : setCookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(cookieName + "=")) {
                return trimmed.substring((cookieName + "=").length());
            }
        }
        fail("Cookie %s not found in header: %s".formatted(cookieName, setCookieHeader));
        return null;
    }
}
