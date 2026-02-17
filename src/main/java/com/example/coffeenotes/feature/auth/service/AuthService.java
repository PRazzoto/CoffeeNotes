package com.example.coffeenotes.feature.auth.service;

import com.example.coffeenotes.domain.auth.AuthRefreshSession;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.catalog.User;
import com.example.coffeenotes.feature.auth.dto.*;
import com.example.coffeenotes.feature.auth.repository.AuthRefreshSessionRepository;
import com.example.coffeenotes.feature.catalog.repository.UserRepository;
import com.example.coffeenotes.security.JwtTokenService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final long REFRESH_TTL =14; // 14 days
    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthRefreshSessionRepository authRefreshSessionRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenService jwtTokenService, RefreshTokenService refreshTokenService) {
        this.authRefreshSessionRepository = authRefreshSessionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    public RegisterReturnDTO register(RegisterRequestDTO registerRequest) {
        if(registerRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fields should not be empty.");
        }
        if(registerRequest.getEmail() == null || registerRequest.getPassword() == null || registerRequest.getDisplayName() == null) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fields should not be empty.");
        }
        if(registerRequest.getEmail().isBlank() || registerRequest.getPassword().isBlank()){

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must contain email and password.");
        }

        if(registerRequest.getDisplayName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must contain display name.");
        }

        if(!patternMatchesEmail(registerRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email not valid.");
        }

        if(!patternMatchesPassword(registerRequest.getPassword())) {
            // The real time validation needs to happen in the FE
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password does not meet the requirements.");
        }

        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();
        if(userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered.");
        }

        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());

        User user = new User();

        user.setPasswordHash(hashedPassword);
        user.setEmail(normalizedEmail);
        user.setDisplayName(registerRequest.getDisplayName().trim());
        user.setRole(Role.USER);

        User saved = userRepository.save(user);

        return toRegisterDTO(saved);
    }


    public AuthLoginResultDTO login(LoginRequestDTO login) {
        if(login== null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fields should not be empty.");
            }
        if(login.getEmail() == null ||login.getPassword() == null) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fields should not be empty.");
        }
        if(login.getEmail().isBlank() || login.getPassword().isBlank()){

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must contain email and password.");
        }
        String normalizedEmail = login.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, login.getPassword()));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials.");
        }
        User user = userRepository.findByEmail(normalizedEmail).
                    orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials."));

        String token = jwtTokenService.generateAccessToken(user);

        String refreshRaw = refreshTokenService.generateRawToken();
        String refreshHash = refreshTokenService.hashToken(refreshRaw);

        LocalDateTime now = LocalDateTime.now();
        AuthRefreshSession session = new AuthRefreshSession();
        session.setUser(user);
        session.setTokenHash(refreshHash);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusDays(REFRESH_TTL));
        session.setRevokedAt(null);

        authRefreshSessionRepository.save(session);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenService.getAccessTtlSeconds());

        return new AuthLoginResultDTO(response, refreshRaw);
    }

    @Transactional  // To avoid lazy loading issues
    public AuthLoginResultDTO refresh(String refreshToken) {
        if(refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token.");
        }

        String hashed = refreshTokenService.hashToken(refreshToken);
        AuthRefreshSession session = authRefreshSessionRepository.findByTokenHashWithLock(hashed)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token."));

        if(session.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token.");
        }

        LocalDateTime now = LocalDateTime.now();
        if(!session.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token.");
        }

        User user = session.getUser();
        session.setRevokedAt(now);
        authRefreshSessionRepository.save(session);

        String newRaw = refreshTokenService.generateRawToken();
        String newHash = refreshTokenService.hashToken(newRaw);

        AuthRefreshSession newSession = new AuthRefreshSession();
        newSession.setUser(user);
        newSession.setTokenHash(newHash);
        newSession.setCreatedAt(now);
        newSession.setExpiresAt(now.plusDays(REFRESH_TTL));
        newSession.setRevokedAt(null);

        authRefreshSessionRepository.save(newSession);

        String token = jwtTokenService.generateAccessToken(user);
        AuthResponseDTO dto = new AuthResponseDTO();
        dto.setAccessToken(token);
        dto.setTokenType("Bearer");
        dto.setExpiresIn(jwtTokenService.getAccessTtlSeconds());

        return new AuthLoginResultDTO(dto, newRaw);
    }

    public void logout(String refreshToken) {

        if(refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String hashed = refreshTokenService.hashToken(refreshToken);
        authRefreshSessionRepository.findByTokenHash(hashed).ifPresent(session -> {
            if(session.getRevokedAt() == null) {
                session.setRevokedAt(LocalDateTime.now());
                authRefreshSessionRepository.save(session);
            }
        });
    }

    private static boolean patternMatchesEmail(String emailAddress) {
        return Pattern.compile("^(.+)@(\\S+)$")
                .matcher(emailAddress)
                .matches();
    }

    private static boolean patternMatchesPassword(String pass) {
        return Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$")
                .matcher(pass)
                .matches();
    }

    public RegisterReturnDTO toRegisterDTO(User user) {
        RegisterReturnDTO returnDTO= new RegisterReturnDTO();
        returnDTO.setDisplayName(user.getDisplayName());
        returnDTO.setEmail(user.getEmail());
        returnDTO.setId(user.getId());
        returnDTO.setRole(user.getRole());

        return returnDTO;
    }
}
