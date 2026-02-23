package com.example.coffeenotes.feature.user.service;

import com.example.coffeenotes.api.dto.user.UpdatePasswordDTO;
import com.example.coffeenotes.api.dto.user.UpdateRequestDTO;
import com.example.coffeenotes.api.dto.user.UserReturnDTO;
import com.example.coffeenotes.domain.auth.AuthRefreshSession;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.auth.repository.AuthRefreshSessionRepository;
import com.example.coffeenotes.feature.catalog.repository.RecipeRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthRefreshSessionRepository authRefreshSessionRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUser_whenUserIdNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUser(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getUser_whenNotFound_throws404() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUser(USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getUser_whenFound_returnsDto() {
        User user = user("patri@coffee.com", "Patri", "hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserReturnDTO dto = userService.getUser(USER_ID);

        assertEquals("patri@coffee.com", dto.getEmail());
        assertEquals("Patri", dto.getDisplayName());
    }

    @Test
    void updateUser_whenUserIdNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(null, new UpdateRequestDTO())
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateUser_whenBodyNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(USER_ID, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateUser_whenUserNotFound_throws404() {
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDisplayName("New Name");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(USER_ID, dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateUser_whenDisplayNameBlank_throws400() {
        User user = user("patri@coffee.com", "Patri", "hash");
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDisplayName("   ");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(USER_ID, dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateUser_whenNameIsTheSame_throws400() {
        User user = user("patri@coffee.com", "Patri", "hash");
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDisplayName("Patri");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(USER_ID, dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateUser_whenValid_updatesAndReturnsDto() {
        User user = user("patri@coffee.com", "Patri", "hash");
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDisplayName("  New Patri  ");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UpdateRequestDTO result = userService.updateUser(USER_ID, dto);

        assertEquals("New Patri", result.getDisplayName());
        assertEquals("New Patri", user.getDisplayName());
        verify(userRepository).save(user);
    }

    @Test
    void updatePassword_whenAnyFieldNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updatePassword(null, USER_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updatePassword_whenBlankFields_throws400() {
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setCurrentPassword(" ");
        dto.setNewPassword(" ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updatePassword(dto, USER_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updatePassword_whenUserNotFound_throws404() {
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setCurrentPassword("Current@123");
        dto.setNewPassword("NewPass@123");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updatePassword(dto, USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updatePassword_whenCurrentPasswordIncorrect_throws400() {
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setCurrentPassword("Wrong@123");
        dto.setNewPassword("NewPass@123");
        User user = user("patri@coffee.com", "Patri", "old-hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong@123", "old-hash")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updatePassword(dto, USER_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updatePassword_whenNewPasswordEqualsCurrent_throws400() {
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setCurrentPassword("Current@123");
        dto.setNewPassword("Current@123");
        User user = user("patri@coffee.com", "Patri", "old-hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current@123", "old-hash")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updatePassword(dto, USER_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updatePassword_whenWeakPassword_throws400() {
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setCurrentPassword("Current@123");
        dto.setNewPassword("weak");
        User user = user("patri@coffee.com", "Patri", "old-hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current@123", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("weak", "old-hash")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updatePassword(dto, USER_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updatePassword_whenValid_updatesHashAndRevokesSessions() {
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setCurrentPassword("Current@123");
        dto.setNewPassword("NewPass@123");
        User user = user("patri@coffee.com", "Patri", "old-hash");

        AuthRefreshSession s1 = new AuthRefreshSession();
        s1.setUser(user);
        s1.setRevokedAt(null);

        AuthRefreshSession s2 = new AuthRefreshSession();
        s2.setUser(user);
        s2.setRevokedAt(null);

        List<AuthRefreshSession> activeSessions = List.of(s1, s2);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current@123", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-hash");
        when(authRefreshSessionRepository.findByUser_IdAndRevokedAtIsNull(USER_ID)).thenReturn(activeSessions);

        userService.updatePassword(dto, USER_ID);

        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(s1.getRevokedAt());
        assertNotNull(s2.getRevokedAt());
        assertEquals(s1.getRevokedAt(), s2.getRevokedAt());
        verify(userRepository).save(user);
        verify(authRefreshSessionRepository).saveAll(activeSessions);
    }

    @Test
    void deleteUser_whenUserIdNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteUser(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(recipeRepository, never()).deleteByOwner_Id(any());
    }

    @Test
    void deleteUser_whenUserNotFound_throws404() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteUser(USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recipeRepository, never()).deleteByOwner_Id(any());
    }

    @Test
    void deleteUser_whenValid_deletesDependenciesThenUser() {
        User user = user("patri@coffee.com", "Patri", "hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.deleteUser(USER_ID);

        verify(recipeRepository).deleteByOwner_Id(USER_ID);
        verify(authRefreshSessionRepository).deleteByUser_Id(USER_ID);
        verify(userRepository).deleteMediaAssetsByOwnerId(USER_ID);
        verify(userRepository).delete(user);
    }

    private User user(String email, String displayName, String passwordHash) {
        return new User(
                USER_ID,
                email,
                passwordHash,
                displayName,
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
