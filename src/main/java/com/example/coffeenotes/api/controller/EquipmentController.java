package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.catalog.EquipmentDTO;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.feature.catalog.service.EquipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return equipments.stream()
                .map(equipment -> {
                    EquipmentDTO dto = new EquipmentDTO();
                    dto.setName(equipment.getName());
                    dto.setDescription(equipment.getDescription());
                    return dto;
                })
                .toList();
    }

    @PostMapping("/createEquipment")
    public ResponseEntity<EquipmentDTO> add(@RequestBody Equipment equipment) {
        Equipment addedEquipment = this.equipmentService.add(equipment);
        EquipmentDTO dto = new EquipmentDTO();
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
        dto.setName(updated.getName());
        dto.setDescription(updated.getDescription());
        return new ResponseEntity<>(dto,HttpStatus.OK);
    }
}
