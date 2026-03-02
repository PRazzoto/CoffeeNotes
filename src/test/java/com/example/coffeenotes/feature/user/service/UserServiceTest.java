package com.example.coffeenotes.feature.user.service;

import com.example.coffeenotes.api.dto.user.UpdatePasswordDTO;
import com.example.coffeenotes.api.dto.user.UpdateRequestDTO;
import com.example.coffeenotes.api.dto.user.UserReturnDTO;
import com.example.coffeenotes.domain.auth.AuthRefreshSession;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import com.example.coffeenotes.domain.catalog.recipe.RecipeVersion;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.auth.repository.AuthRefreshSessionRepository;
import com.example.coffeenotes.feature.catalog.repository.CoffeeBeanRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeEquipmentRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeTrackRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeVersionRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeWaterPourRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TRACK_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VERSION_2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthRefreshSessionRepository authRefreshSessionRepository;

    @Mock
    private RecipeTrackRepository recipeTrackRepository;

    @Mock
    private RecipeVersionRepository recipeVersionRepository;

    @Mock
    private RecipeWaterPourRepository recipeWaterPourRepository;

    @Mock
    private RecipeEquipmentRepository recipeEquipmentRepository;

    @Mock
    private CoffeeBeanRepository coffeeBeanRepository;

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
        verify(recipeTrackRepository, never()).findAllByOwner_Id(any());
    }

    @Test
    void deleteUser_whenUserNotFound_throws404() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteUser(USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recipeTrackRepository, never()).findAllByOwner_Id(any());
    }

    @Test
    void deleteUser_whenValid_deletesVersionedGraphAndUser() {
        User user = user("patri@coffee.com", "Patri", "hash");

        RecipeTrack ownedTrack = new RecipeTrack();
        ownedTrack.setId(TRACK_ID);
        ownedTrack.setOwner(user);

        RecipeVersion v1 = new RecipeVersion();
        v1.setId(VERSION_1);
        RecipeVersion v2 = new RecipeVersion();
        v2.setId(VERSION_2);

        CoffeeBean ownedBean = new CoffeeBean();
        ownedBean.setId(UUID.randomUUID());
        ownedBean.setOwner(user);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(recipeTrackRepository.findAllByOwner_Id(USER_ID)).thenReturn(List.of(ownedTrack));
        when(recipeVersionRepository.findByTrack_IdIn(List.of(TRACK_ID))).thenReturn(List.of(v1, v2));
        when(coffeeBeanRepository.findAllByOwner_Id(USER_ID)).thenReturn(List.of(ownedBean));

        userService.deleteUser(USER_ID);

        verify(recipeWaterPourRepository).deleteByRecipeVersion_IdIn(argThat(ids ->
                ids.containsAll(List.of(VERSION_1, VERSION_2)) && ids.size() == 2));
        verify(recipeEquipmentRepository).deleteByRecipeVersion_IdIn(argThat(ids ->
                ids.containsAll(List.of(VERSION_1, VERSION_2)) && ids.size() == 2));

        ArgumentCaptor<List<RecipeVersion>> versionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(recipeVersionRepository).deleteAll(versionsCaptor.capture());
        assertEquals(2, versionsCaptor.getValue().size());

        ArgumentCaptor<List<RecipeTrack>> trackCaptor = ArgumentCaptor.forClass(List.class);
        verify(recipeTrackRepository).deleteAll(trackCaptor.capture());
        assertEquals(1, trackCaptor.getValue().size());
        assertEquals(TRACK_ID, trackCaptor.getValue().get(0).getId());

        ArgumentCaptor<List<CoffeeBean>> beanCaptor = ArgumentCaptor.forClass(List.class);
        verify(coffeeBeanRepository).deleteAll(beanCaptor.capture());
        assertEquals(1, beanCaptor.getValue().size());
        assertEquals(ownedBean.getId(), beanCaptor.getValue().get(0).getId());

        verify(authRefreshSessionRepository).deleteByUser_Id(USER_ID);
        verify(userRepository).deleteMediaAssetsByOwnerId(USER_ID);
        verify(userRepository).delete(user);
    }

    private User user(String email, String displayName, String passwordHash) {
        return user(USER_ID, email, displayName, passwordHash);
    }

    private User user(UUID id, String email, String displayName, String passwordHash) {
        return new User(
                id,
                email,
                passwordHash,
                displayName,
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
