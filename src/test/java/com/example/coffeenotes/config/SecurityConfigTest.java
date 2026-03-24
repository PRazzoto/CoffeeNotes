package com.example.coffeenotes.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void jwtAuthenticationConverter_mapsAdminRoleClaimToRoleAdminAuthority() {
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("role", "ADMIN")
                .subject("11111111-1111-1111-1111-111111111111")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void jwtAuthenticationConverter_mapsUserRoleClaimToRoleUserAuthority() {
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("role", "USER")
                .subject("11111111-1111-1111-1111-111111111111")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
    }
}
