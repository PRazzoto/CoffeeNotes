package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.recipe.FavoriteResponseDTO;
import com.example.coffeenotes.api.dto.recipe.TrackSummaryResponseDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.catalog.Favorite;
import com.example.coffeenotes.domain.catalog.FavoriteId;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.FavoriteRepository;
import com.example.coffeenotes.feature.catalog.repository.recipe.RecipeTrackRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TRACK_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BEAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID METHOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private RecipeTrackRepository recipeTrackRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void addFavorite_whenTrackVisibleAndNotAlreadyFavorited_returnsFavoriteTrueAndSaves() {
        User owner = user(USER_ID, "owner@test.com");
        RecipeTrack track = track(TRACK_ID, owner, "Mine", false, null);

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(favoriteRepository.existsByUser_IdAndRecipeTrack_Id(USER_ID, TRACK_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FavoriteResponseDTO out = favoriteService.addFavorite(USER_ID, TRACK_ID);

        assertEquals(TRACK_ID, out.getTrackId());
        assertTrue(out.isFavorite());
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void addFavorite_whenAlreadyFavorited_returnsFavoriteTrueWithoutSavingDuplicate() {
        User owner = user(USER_ID, "owner@test.com");
        RecipeTrack track = track(TRACK_ID, owner, "Mine", false, null);

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(favoriteRepository.existsByUser_IdAndRecipeTrack_Id(USER_ID, TRACK_ID)).thenReturn(true);

        FavoriteResponseDTO out = favoriteService.addFavorite(USER_ID, TRACK_ID);

        assertEquals(TRACK_ID, out.getTrackId());
        assertTrue(out.isFavorite());
        verify(favoriteRepository, never()).save(any(Favorite.class));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void addFavorite_whenTrackMissing_throws404() {
        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> favoriteService.addFavorite(USER_ID, TRACK_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void addFavorite_whenTrackDeleted_throws404() {
        User owner = user(USER_ID, "owner@test.com");
        RecipeTrack track = track(TRACK_ID, owner, "Deleted", false, LocalDateTime.now());

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> favoriteService.addFavorite(USER_ID, TRACK_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void addFavorite_whenTrackHidden_throws404() {
        User owner = user(OTHER_USER_ID, "other@test.com");
        RecipeTrack track = track(TRACK_ID, owner, "Private", false, null);

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> favoriteService.addFavorite(USER_ID, TRACK_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void removeFavorite_whenAlreadyFavorited_returnsFavoriteFalseAndDeletes() {
        User owner = user(USER_ID, "owner@test.com");
        RecipeTrack track = track(TRACK_ID, owner, "Mine", false, null);

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(favoriteRepository.existsByUser_IdAndRecipeTrack_Id(USER_ID, TRACK_ID)).thenReturn(true);

        FavoriteResponseDTO out = favoriteService.removeFavorite(USER_ID, TRACK_ID);

        assertEquals(TRACK_ID, out.getTrackId());
        assertFalse(out.isFavorite());
        verify(favoriteRepository).deleteByUser_IdAndRecipeTrack_Id(USER_ID, TRACK_ID);
    }

    @Test
    void removeFavorite_whenVisibleTrackButNotFavorited_returnsFavoriteFalseWithoutDelete() {
        User owner = user(USER_ID, "owner@test.com");
        RecipeTrack track = track(TRACK_ID, owner, "Mine", false, null);

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(favoriteRepository.existsByUser_IdAndRecipeTrack_Id(USER_ID, TRACK_ID)).thenReturn(false);

        FavoriteResponseDTO out = favoriteService.removeFavorite(USER_ID, TRACK_ID);

        assertEquals(TRACK_ID, out.getTrackId());
        assertFalse(out.isFavorite());
        verify(favoriteRepository, never()).deleteByUser_IdAndRecipeTrack_Id(any(), any());
    }

    @Test
    void removeFavorite_whenTrackDeleted_throws404() {
        User owner = user(USER_ID, "owner@test.com");
        RecipeTrack track = track(TRACK_ID, owner, "Deleted", false, LocalDateTime.now());

        when(recipeTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> favoriteService.removeFavorite(USER_ID, TRACK_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(favoriteRepository, never()).deleteByUser_IdAndRecipeTrack_Id(any(), any());
    }

    @Test
    void listFavoriteRecipes_whenUserIdNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> favoriteService.listFavoriteRecipes(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void listFavoriteRecipes_whenValid_returnsOnlyVisibleActiveTracks() {
        User owner = user(USER_ID, "owner@test.com");
        User other = user(OTHER_USER_ID, "other@test.com");

        RecipeTrack visibleOwn = track(TRACK_ID, owner, "Own Favorite", false, null);
        RecipeTrack visibleGlobal = track(UUID.randomUUID(), other, "Global Favorite", true, null);
        RecipeTrack hidden = track(UUID.randomUUID(), other, "Hidden Favorite", false, null);
        RecipeTrack deleted = track(UUID.randomUUID(), owner, "Deleted Favorite", false, LocalDateTime.now());

        when(favoriteRepository.findByUser_Id(USER_ID)).thenReturn(List.of(
                favorite(owner, visibleOwn),
                favorite(owner, visibleGlobal),
                favorite(owner, hidden),
                favorite(owner, deleted)
        ));

        List<TrackSummaryResponseDTO> out = favoriteService.listFavoriteRecipes(USER_ID);

        assertEquals(2, out.size());
        assertTrue(out.stream().anyMatch(dto -> dto.getTrackId().equals(visibleOwn.getId()) && dto.isFavorite()));
        assertTrue(out.stream().anyMatch(dto -> dto.getTrackId().equals(visibleGlobal.getId()) && dto.isFavorite()));
        assertFalse(out.stream().anyMatch(dto -> dto.getTrackId().equals(hidden.getId())));
        assertFalse(out.stream().anyMatch(dto -> dto.getTrackId().equals(deleted.getId())));
    }

    @Test
    void listFavoriteRecipes_whenVisibleTrackReturned_mapsSummaryFields() {
        User owner = user(USER_ID, "owner@test.com");
        RecipeTrack track = track(TRACK_ID, owner, "Morning Favorite", true, null);

        when(favoriteRepository.findByUser_Id(USER_ID)).thenReturn(List.of(favorite(owner, track)));

        List<TrackSummaryResponseDTO> out = favoriteService.listFavoriteRecipes(USER_ID);

        assertEquals(1, out.size());
        TrackSummaryResponseDTO dto = out.get(0);
        assertEquals(TRACK_ID, dto.getTrackId());
        assertEquals("Morning Favorite", dto.getTitle());
        assertTrue(dto.isGlobal());
        assertTrue(dto.isFavorite());
        assertNotNull(dto.getUpdatedAt());
    }

    private User user(UUID id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setDisplayName("Name");
        u.setRole(Role.USER);
        return u;
    }

    private RecipeTrack track(UUID id, User owner, String title, boolean global, LocalDateTime deletedAt) {
        CoffeeBean bean = new CoffeeBean();
        bean.setId(BEAN_ID);
        bean.setOwner(owner);
        bean.setName("Bean");
        bean.setGlobal(global);

        BrewMethods method = new BrewMethods();
        method.setId(METHOD_ID);
        method.setName("V60");

        RecipeTrack track = new RecipeTrack();
        track.setId(id);
        track.setOwner(owner);
        track.setBean(bean);
        track.setMethod(method);
        track.setTitle(title);
        track.setGlobal(global);
        track.setDeletedAt(deletedAt);
        track.setUpdatedAt(LocalDateTime.now());
        return track;
    }

    private Favorite favorite(User user, RecipeTrack track) {
        Favorite favorite = new Favorite();
        favorite.setId(new FavoriteId(user.getId(), track.getId()));
        favorite.setUser(user);
        favorite.setRecipeTrack(track);
        return favorite;
    }
}
