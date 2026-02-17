package com.example.coffeenotes.security;

import com.example.coffeenotes.domain.catalog.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final long ACCESS_TTL_SECONDS = 900; // 15 min

    private final JwtEncoder jwtEncoder;

    @Value("${app.security.jwt.issuer}")
    private String issuer;

    @Value("${app.security.jwt.audience}")
    private String audience;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TTL_SECONDS))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    }

    public long getAccessTtlSeconds() {
        return ACCESS_TTL_SECONDS;
    }
}
