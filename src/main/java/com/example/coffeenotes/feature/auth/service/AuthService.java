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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
public class AuthService {
    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthService(AuthRefreshSessionRepository authRefreshSessionRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.authRefreshSessionRepository = authRefreshSessionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
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


    public AuthResponseDTO login(LoginRequestDTO login) {
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

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenService.getAccessTtlSeconds());

        return response;
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
