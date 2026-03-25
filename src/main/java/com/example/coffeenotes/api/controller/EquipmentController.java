package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.catalog.EquipmentDTO;
import com.example.coffeenotes.api.dto.catalog.UserEquipmentSelectionDTO;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.feature.catalog.service.EquipmentService;
import com.example.coffeenotes.util.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/listAll")
    public List<EquipmentDTO> allEquipments() {
        List<Equipment> equipments = equipmentService.listAllEquipments();
        return mapEquipments(equipments);
    }

    @GetMapping("/mine")
    public List<EquipmentDTO> myEquipments(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        return mapEquipments(equipmentService.listMyEquipments(userId));
    }

    @PutMapping("/mine")
    public List<EquipmentDTO> replaceMyEquipments(@AuthenticationPrincipal Jwt jwt, @RequestBody UserEquipmentSelectionDTO body) {
        UUID userId = JwtUtils.extractUserId(jwt);
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required.");
        }
        return mapEquipments(equipmentService.replaceMyEquipments(userId, body.getEquipmentIds()));
    }

    @PostMapping("/createEquipment")
    public ResponseEntity<EquipmentDTO> add(@RequestBody Equipment equipment) {
        Equipment addedEquipment = this.equipmentService.add(equipment);
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(addedEquipment.getId());
        dto.setName(addedEquipment.getName());
        dto.setDescription(addedEquipment.getDescription());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @DeleteMapping("/deleteEquipment/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id){
        equipmentService.delete(id);
    }

    @PutMapping("/editEquipment/{id}")
    public ResponseEntity<EquipmentDTO> updateEquipment(@PathVariable UUID id, @RequestBody EquipmentDTO body){
        Equipment updated = equipmentService.update(id, body);
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(updated.getId());
        dto.setName(updated.getName());
        dto.setDescription(updated.getDescription());
        return new ResponseEntity<>(dto,HttpStatus.OK);
    }

    private List<EquipmentDTO> mapEquipments(List<Equipment> equipments) {
        return equipments.stream()
                .map(equipment -> {
                    EquipmentDTO dto = new EquipmentDTO();
                    dto.setId(equipment.getId());
                    dto.setName(equipment.getName());
                    dto.setDescription(equipment.getDescription());
                    return dto;
                })
                .toList();
    }
}
