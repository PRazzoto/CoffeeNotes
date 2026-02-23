package com.example.coffeenotes.security;

import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private JwtTokenService jwtTokenService;

    @Test
    void generateAccessToken_returnsEncodedValue() {
        ReflectionTestUtils.setField(jwtTokenService, "issuer", "coffeenotes-api");
        ReflectionTestUtils.setField(jwtTokenService, "audience", "coffeenotes-web");

        User user = new User(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "test@coffee.com",
                "hashed",
                "Test User",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Jwt jwt = new Jwt(
                "encoded-token",
                Instant.now(),
                Instant.now().plusSeconds(900),
                Map.of("alg", "RS256"),
                Map.of("sub", user.getId().toString())
        );
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        String token = jwtTokenService.generateAccessToken(user);

        assertEquals("encoded-token", token);
        assertEquals(900L, jwtTokenService.getAccessTtlSeconds());
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void generateAccessToken_containsExpectedClaims() {
        ReflectionTestUtils.setField(jwtTokenService, "issuer", "coffeenotes-api");
        ReflectionTestUtils.setField(jwtTokenService, "audience", "coffeenotes-web");

        User user = new User(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "test@coffee.com",
                "hashed",
                "Test User",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Jwt jwt = new Jwt(
                "encoded-token",
                Instant.now(),
                Instant.now().plusSeconds(900),
                Map.of("alg", "RS256"),
                Map.of("sub", user.getId().toString())
        );
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        jwtTokenService.generateAccessToken(user);

        ArgumentCaptor<JwtEncoderParameters> paramsCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(paramsCaptor.capture());

        assertEquals("coffeenotes-api", paramsCaptor.getValue().getClaims().getClaim("iss"));
        assertEquals("coffeenotes-web", paramsCaptor.getValue().getClaims().getAudience().get(0));
        assertEquals(user.getId().toString(), paramsCaptor.getValue().getClaims().getSubject());
        assertEquals("test@coffee.com", paramsCaptor.getValue().getClaims().getClaim("email"));
        assertEquals("USER", paramsCaptor.getValue().getClaims().getClaim("role"));
    }
}
