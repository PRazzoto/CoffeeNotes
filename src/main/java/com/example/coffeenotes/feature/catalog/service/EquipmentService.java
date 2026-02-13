package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.EquipmentDTO;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.feature.catalog.repository.EquipmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;

    public List<Equipment> listAllEquipments() {
        return equipmentRepository.findAll();
    }

    public Equipment add(Equipment equipment) {
        if(equipment.getName() == null || equipment.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        return equipmentRepository.save(equipment);
    }

    public void delete(UUID id){
        if(!equipmentRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found");
        }

        equipmentRepository.deleteById(id);
    }

    public Equipment update(UUID id, EquipmentDTO body){
        Equipment existing = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        if(body.getName()==null && body.getDescription()==null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name or Description is required");
        }
        if(body.getName() != null) {
            if(body.getName().isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be blank");
            }
            existing.setName(body.getName());
        }
        if(body.getDescription() != null) {
            if(body.getDescription().isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must not be blank");
            }
           existing.setDescription(body.getDescription());
        }
        return equipmentRepository.save(existing);
    }

}
