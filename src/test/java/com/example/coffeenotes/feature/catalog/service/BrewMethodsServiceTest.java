package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.BrewMethodsDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.feature.catalog.repository.BrewMethodsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrewMethodsServiceTest {
    private static final UUID ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_10 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private BrewMethodsRepository brewMethodsRepository;

    @InjectMocks
    private BrewMethodsService brewMethodsService;

    @Test
    void update_whenNotFound_throws404() {
        when(brewMethodsRepository.findById(ID_1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> brewMethodsService.update(ID_1, new BrewMethodsDTO())
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void update_whenEmptyBody_throws400() {
        BrewMethods existing = new BrewMethods(ID_1, "V60", "Cone dripper");
        when(brewMethodsRepository.findById(ID_1)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> brewMethodsService.update(ID_1, new BrewMethodsDTO())
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_whenPartial_updatesOnlyProvidedFields() {
        BrewMethods existing = new BrewMethods(ID_1, "V60", "Cone dripper");
        when(brewMethodsRepository.findById(ID_1)).thenReturn(Optional.of(existing));
        when(brewMethodsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BrewMethodsDTO body = new BrewMethodsDTO();
        body.setName("Kalita Wave");

        BrewMethods updated = brewMethodsService.update(ID_1, body);

        assertEquals("Kalita Wave", updated.getName());
        assertEquals("Cone dripper", updated.getDescription());

        ArgumentCaptor<BrewMethods> captor = ArgumentCaptor.forClass(BrewMethods.class);
        verify(brewMethodsRepository).save(captor.capture());
        assertEquals("Kalita Wave", captor.getValue().getName());
        assertEquals("Cone dripper", captor.getValue().getDescription());
    }

    @Test
    void update_whenDescriptionOnly_updatesOnlyDescription() {
        BrewMethods existing = new BrewMethods(ID_1, "V60", "Cone dripper");
        when(brewMethodsRepository.findById(ID_1)).thenReturn(Optional.of(existing));
        when(brewMethodsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BrewMethodsDTO body = new BrewMethodsDTO();
        body.setDescription("Flat-bottom dripper");

        BrewMethods updated = brewMethodsService.update(ID_1, body);

        assertEquals("V60", updated.getName());
        assertEquals("Flat-bottom dripper", updated.getDescription());
        verify(brewMethodsRepository).save(any());
    }

    @Test
    void update_whenBothFieldsProvided_updatesBoth() {
        BrewMethods existing = new BrewMethods(ID_1, "V60", "Cone dripper");
        when(brewMethodsRepository.findById(ID_1)).thenReturn(Optional.of(existing));
        when(brewMethodsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BrewMethodsDTO body = new BrewMethodsDTO();
        body.setName("AeroPress");
        body.setDescription("Immersion");

        BrewMethods updated = brewMethodsService.update(ID_1, body);

        assertEquals("AeroPress", updated.getName());
        assertEquals("Immersion", updated.getDescription());
        verify(brewMethodsRepository).save(any());
    }

    @Test
    void update_whenBlankName_throws400() {
        BrewMethods existing = new BrewMethods(ID_1, "V60", "Cone dripper");
        when(brewMethodsRepository.findById(ID_1)).thenReturn(Optional.of(existing));

        BrewMethodsDTO body = new BrewMethodsDTO();
        body.setName("   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> brewMethodsService.update(ID_1, body)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_whenBlankDescription_throws400() {
        BrewMethods existing = new BrewMethods(ID_1, "V60", "Cone dripper");
        when(brewMethodsRepository.findById(ID_1)).thenReturn(Optional.of(existing));

        BrewMethodsDTO body = new BrewMethodsDTO();
        body.setDescription("   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> brewMethodsService.update(ID_1, body)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void add_savesBrewMethods() {
        BrewMethods input = new BrewMethods(null, "French Press", "Immersion");
        BrewMethods saved = new BrewMethods(ID_10, "French Press", "Immersion");
        when(brewMethodsRepository.save(input)).thenReturn(saved);

        BrewMethods result = brewMethodsService.add(input);

        assertEquals(ID_10, result.getId());
        assertEquals("French Press", result.getName());
        assertEquals("Immersion", result.getDescription());
        verify(brewMethodsRepository).save(input);
    }

    @Test
    void add_whenBlankName_throws400() {
        BrewMethods input = new BrewMethods(null, "   ", "Immersion");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> brewMethodsService.add(input)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(brewMethodsRepository, never()).save(any());
    }

    @Test
    void add_whenBlankDescription_savesBrewMethod() {
        BrewMethods input = new BrewMethods(null, "French Press", "   ");
        BrewMethods saved = new BrewMethods(ID_10, "French Press", "   ");
        when(brewMethodsRepository.save(input)).thenReturn(saved);

        BrewMethods result = brewMethodsService.add(input);

        assertEquals(ID_10, result.getId());
        assertEquals("French Press", result.getName());
        assertEquals("   ", result.getDescription());
        verify(brewMethodsRepository).save(input);
    }

    @Test
    void delete_whenNotFound_throws404() {
        when(brewMethodsRepository.existsById(ID_1)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> brewMethodsService.delete(ID_1)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(brewMethodsRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void delete_whenFound_deletes() {
        when(brewMethodsRepository.existsById(ID_1)).thenReturn(true);

        brewMethodsService.delete(ID_1);

        verify(brewMethodsRepository).deleteById(ID_1);
    }
}
