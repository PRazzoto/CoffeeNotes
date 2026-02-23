package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.catalog.EquipmentDTO;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.feature.catalog.repository.EquipmentRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {
    private static final UUID ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_10 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private EquipmentService equipmentService;

    @Test
    void update_whenNotFound_throws404() {
        when(equipmentRepository.findById(ID_1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> equipmentService.update(ID_1, new EquipmentDTO())
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void update_whenEmptyBody_throws400() {
        Equipment existing = new Equipment(ID_1, "Grinder", "Old");
        when(equipmentRepository.findById(ID_1)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> equipmentService.update(ID_1, new EquipmentDTO())
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_whenPartial_updatesOnlyProvidedFields() {
        Equipment existing = new Equipment(ID_1, "Grinder", "Old");
        when(equipmentRepository.findById(ID_1)).thenReturn(Optional.of(existing));
        when(equipmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EquipmentDTO body = new EquipmentDTO();
        body.setName("New Name");

        Equipment updated = equipmentService.update(ID_1, body);

        assertEquals("New Name", updated.getName());
        assertEquals("Old", updated.getDescription());

        ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentRepository).save(captor.capture());
        assertEquals("New Name", captor.getValue().getName());
        assertEquals("Old", captor.getValue().getDescription());
    }

    @Test
    void update_whenDescriptionOnly_updatesOnlyDescription() {
        Equipment existing = new Equipment(ID_1, "Grinder", "Old");
        when(equipmentRepository.findById(ID_1)).thenReturn(Optional.of(existing));
        when(equipmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EquipmentDTO body = new EquipmentDTO();
        body.setDescription("New Desc");

        Equipment updated = equipmentService.update(ID_1, body);

        assertEquals("Grinder", updated.getName());
        assertEquals("New Desc", updated.getDescription());
        verify(equipmentRepository).save(any());
    }

    @Test
    void update_whenBothFieldsProvided_updatesBoth() {
        Equipment existing = new Equipment(ID_1, "Grinder", "Old");
        when(equipmentRepository.findById(ID_1)).thenReturn(Optional.of(existing));
        when(equipmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EquipmentDTO body = new EquipmentDTO();
        body.setName("New Name");
        body.setDescription("New Desc");

        Equipment updated = equipmentService.update(ID_1, body);

        assertEquals("New Name", updated.getName());
        assertEquals("New Desc", updated.getDescription());
        verify(equipmentRepository).save(any());
    }

    @Test
    void update_whenBlankName_throws400() {
        Equipment existing = new Equipment(ID_1, "Grinder", "Old");
        when(equipmentRepository.findById(ID_1)).thenReturn(Optional.of(existing));

        EquipmentDTO body = new EquipmentDTO();
        body.setName("   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> equipmentService.update(ID_1, body)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_whenBlankDescription_throws400() {
        Equipment existing = new Equipment(ID_1, "Grinder", "Old");
        when(equipmentRepository.findById(ID_1)).thenReturn(Optional.of(existing));

        EquipmentDTO body = new EquipmentDTO();
        body.setDescription("   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> equipmentService.update(ID_1, body)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void add_savesEquipment() {
        Equipment input = new Equipment(null, "Kettle", "Stovetop");
        Equipment saved = new Equipment(ID_10, "Kettle", "Stovetop");
        when(equipmentRepository.save(input)).thenReturn(saved);

        Equipment result = equipmentService.add(input);

        assertEquals(ID_10, result.getId());
        assertEquals("Kettle", result.getName());
        assertEquals("Stovetop", result.getDescription());
        verify(equipmentRepository).save(input);
    }

    @Test
    void delete_whenNotFound_throws404() {
        when(equipmentRepository.existsById(ID_1)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> equipmentService.delete(ID_1)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(equipmentRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void delete_whenFound_deletes() {
        when(equipmentRepository.existsById(ID_1)).thenReturn(true);

        equipmentService.delete(ID_1);

        verify(equipmentRepository).deleteById(ID_1);
    }
}
