package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.feature.catalog.repository.EquipmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CatalogService {
    private final EquipmentRepository equipmentRepository;

    public List<Equipment> listAllEquipments() {
        return equipmentRepository.findAll();
    }

    public Equipment add(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }
}
