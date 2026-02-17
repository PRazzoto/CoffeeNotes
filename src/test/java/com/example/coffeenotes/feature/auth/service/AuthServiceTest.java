package com.example.coffeenotes.feature.auth.service;

import com.example.coffeenotes.domain.auth.AuthRefreshSession;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.catalog.User;
import com.example.coffeenotes.feature.auth.dto.AuthLoginResultDTO;
import com.example.coffeenotes.feature.auth.dto.AuthResponseDTO;
import com.example.coffeenotes.feature.auth.dto.LoginRequestDTO;
import com.example.coffeenotes.feature.auth.dto.RegisterRequestDTO;
import com.example.coffeenotes.feature.auth.dto.RegisterReturnDTO;
import com.example.coffeenotes.feature.auth.repository.AuthRefreshSessionRepository;
import com.example.coffeenotes.feature.catalog.repository.UserRepository;
import com.example.coffeenotes.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private AuthRefreshSessionRepository authRefreshSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_whenNullBody_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void register_whenDuplicateEmail_throws409() {
        RegisterRequestDTO body = registerRequest("Test@Coffee.com", "Strong@123", "Patri");
        when(userRepository.findByEmail("test@coffee.com")).thenReturn(Optional.of(new User()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(body)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenWeakPassword_throws400() {
        RegisterRequestDTO body = registerRequest("test@coffee.com", "weak", "Patri");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(body)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenInvalidEmail_throws400() {
        RegisterRequestDTO body = registerRequest("not-an-email", "Strong@123", "Patri");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(body)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenEmailHasSpaceAfterAt_throws400() {
        RegisterRequestDTO body = registerRequest("test@ coffee.com", "Strong@123", "Patri");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(body)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenBlankDisplayName_throws400() {
        RegisterRequestDTO body = registerRequest("test@coffee.com", "Strong@123", "   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(body)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenValid_normalizesAndSaves() {
        RegisterRequestDTO body = registerRequest("Test@Coffee.com", "Strong@123", "  Patri  ");

        User saved = new User(
                USER_ID,
                "test@coffee.com",
                "hashed",
                "Patri",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userRepository.findByEmail("test@coffee.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Strong@123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        RegisterReturnDTO result = authService.register(body);

        assertEquals(USER_ID, result.getId());
        assertEquals("test@coffee.com", result.getEmail());
        assertEquals("Patri", result.getDisplayName());
        assertEquals(Role.USER, result.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("test@coffee.com", userCaptor.getValue().getEmail());
        assertEquals("Patri", userCaptor.getValue().getDisplayName());
        assertEquals("hashed", userCaptor.getValue().getPasswordHash());
        assertEquals(Role.USER, userCaptor.getValue().getRole());
    }

    @Test
    void login_whenNullBody_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void login_whenBlankEmail_throws400() {
        LoginRequestDTO body = loginRequest(" ", "Strong@123");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(body)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void login_whenAuthFails_throws401() {
        LoginRequestDTO body = loginRequest("test@coffee.com", "Wrong@123");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad creds"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(body)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_whenUserNotFoundAfterAuth_throws401() {
        LoginRequestDTO body = loginRequest("test@coffee.com", "Strong@123");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@coffee.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(body)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_whenValid_usesNormalizedEmailForAuthentication() {
        LoginRequestDTO body = loginRequest("  Test@Coffee.com ", "Strong@123");
        User user = new User(
                USER_ID,
                "test@coffee.com",
                "hashed",
                "Patri",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@coffee.com")).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtTokenService.getAccessTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.generateRawToken()).thenReturn("refresh-token");
        when(refreshTokenService.hashToken("refresh-token")).thenReturn("refresh-hash");

        authService.login(body);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertEquals("test@coffee.com", authCaptor.getValue().getPrincipal());
        assertEquals("Strong@123", authCaptor.getValue().getCredentials());
    }

    @Test
    void login_whenValid_returnsTokenPayload() {
        LoginRequestDTO body = loginRequest("  Test@Coffee.com ", "Strong@123");
        User user = new User(
                USER_ID,
                "test@coffee.com",
                "hashed",
                "Patri",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@coffee.com")).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtTokenService.getAccessTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.generateRawToken()).thenReturn("refresh-token");
        when(refreshTokenService.hashToken("refresh-token")).thenReturn("refresh-hash");

        AuthLoginResultDTO result = authService.login(body);
        AuthResponseDTO response = result.getAuthResponse();

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertEquals("refresh-token", result.getRefreshToken());
    }

    @Test
    void refresh_whenTokenMissing_throws401() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh(null)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(authRefreshSessionRepository);
    }

    @Test
    void refresh_whenTokenNotFound_throws401() {
        when(refreshTokenService.hashToken("raw-token")).thenReturn("old-hash");
        when(authRefreshSessionRepository.findByTokenHashWithLock("old-hash")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh("raw-token")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void refresh_whenSessionRevoked_throws401() {
        AuthRefreshSession session = new AuthRefreshSession();
        session.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenService.hashToken("raw-token")).thenReturn("old-hash");
        when(authRefreshSessionRepository.findByTokenHashWithLock("old-hash")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh("raw-token")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void refresh_whenSessionExpired_throws401() {
        AuthRefreshSession session = new AuthRefreshSession();
        session.setRevokedAt(null);
        session.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(refreshTokenService.hashToken("raw-token")).thenReturn("old-hash");
        when(authRefreshSessionRepository.findByTokenHashWithLock("old-hash")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh("raw-token")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void refresh_whenValid_rotatesSessionAndReturnsNewTokens() {
        User user = new User(
                USER_ID,
                "test@coffee.com",
                "hashed",
                "Patri",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        AuthRefreshSession oldSession = new AuthRefreshSession();
        oldSession.setUser(user);
        oldSession.setTokenHash("old-hash");
        oldSession.setCreatedAt(LocalDateTime.now().minusDays(1));
        oldSession.setExpiresAt(LocalDateTime.now().plusDays(1));
        oldSession.setRevokedAt(null);

        when(refreshTokenService.hashToken("old-raw")).thenReturn("old-hash");
        when(authRefreshSessionRepository.findByTokenHashWithLock("old-hash")).thenReturn(Optional.of(oldSession));
        when(refreshTokenService.generateRawToken()).thenReturn("new-raw");
        when(refreshTokenService.hashToken("new-raw")).thenReturn("new-hash");
        when(jwtTokenService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtTokenService.getAccessTtlSeconds()).thenReturn(900L);

        AuthLoginResultDTO result = authService.refresh("old-raw");

        assertEquals("new-raw", result.getRefreshToken());
        assertEquals("jwt-token", result.getAuthResponse().getAccessToken());
        assertEquals("Bearer", result.getAuthResponse().getTokenType());
        assertEquals(900L, result.getAuthResponse().getExpiresIn());

        ArgumentCaptor<AuthRefreshSession> sessionCaptor = ArgumentCaptor.forClass(AuthRefreshSession.class);
        verify(authRefreshSessionRepository, times(2)).save(sessionCaptor.capture());

        List<AuthRefreshSession> saved = sessionCaptor.getAllValues();
        AuthRefreshSession revoked = saved.get(0);
        AuthRefreshSession rotated = saved.get(1);

        assertEquals("old-hash", revoked.getTokenHash());
        assertEquals(user, revoked.getUser());
        assertEquals("new-hash", rotated.getTokenHash());
        assertEquals(user, rotated.getUser());
        assertNull(rotated.getRevokedAt());
        assertEquals(rotated.getCreatedAt().plusDays(14), rotated.getExpiresAt());
    }

    @Test
    void logout_whenTokenMissing_doesNothing() {
        authService.logout(" ");
        verifyNoInteractions(authRefreshSessionRepository);
    }

    @Test
    void logout_whenTokenNotFound_doesNothing() {
        when(refreshTokenService.hashToken("raw-token")).thenReturn("hash");
        when(authRefreshSessionRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        authService.logout("raw-token");

        verify(authRefreshSessionRepository).findByTokenHash("hash");
        verify(authRefreshSessionRepository, never()).save(any(AuthRefreshSession.class));
    }

    @Test
    void logout_whenSessionActive_revokesSession() {
        AuthRefreshSession session = new AuthRefreshSession();
        session.setRevokedAt(null);

        when(refreshTokenService.hashToken("raw-token")).thenReturn("hash");
        when(authRefreshSessionRepository.findByTokenHash("hash")).thenReturn(Optional.of(session));

        authService.logout("raw-token");

        verify(authRefreshSessionRepository).save(session);
    }

    @Test
    void logout_whenSessionAlreadyRevoked_doesNotSave() {
        AuthRefreshSession session = new AuthRefreshSession();
        session.setRevokedAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenService.hashToken("raw-token")).thenReturn("hash");
        when(authRefreshSessionRepository.findByTokenHash("hash")).thenReturn(Optional.of(session));

        authService.logout("raw-token");

        verify(authRefreshSessionRepository, never()).save(any(AuthRefreshSession.class));
    }

    private RegisterRequestDTO registerRequest(String email, String password, String displayName) {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setDisplayName(displayName);
        return dto;
    }

    private LoginRequestDTO loginRequest(String email, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }
}
