package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.EquipmentDTO;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.feature.catalog.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog")
class CatalogController {

    private final CatalogService catalogService;

    CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/listAll")
    public List<EquipmentDTO> allEquipments() {
        List<Equipment> equipments = catalogService.listAllEquipments();
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
    public ResponseEntity<Equipment> add(@RequestBody Equipment equipment) {
        Equipment addedEquipment = this.catalogService.add(equipment);
        return new ResponseEntity<>(addedEquipment, HttpStatus.CREATED);
    }


}
