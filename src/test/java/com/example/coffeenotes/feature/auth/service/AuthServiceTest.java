package com.example.coffeenotes.feature.auth.service;

import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.catalog.User;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

        AuthResponseDTO result = authService.login(body);

        assertEquals("jwt-token", result.getAccessToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(900L, result.getExpiresIn());
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
