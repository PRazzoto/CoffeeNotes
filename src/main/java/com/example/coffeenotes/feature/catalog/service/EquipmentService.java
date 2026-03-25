package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.catalog.EquipmentDTO;
import com.example.coffeenotes.domain.catalog.Equipment;
import com.example.coffeenotes.domain.catalog.UserEquipment;
import com.example.coffeenotes.domain.catalog.UserEquipmentId;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.EquipmentRepository;
import com.example.coffeenotes.feature.catalog.repository.UserEquipmentRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final UserEquipmentRepository userEquipmentRepository;
    private final UserRepository userRepository;

    public List<Equipment> listAllEquipments() {
        return equipmentRepository.findAll();
    }

    public Equipment add(Equipment equipment) {
        if(equipment.getName() == null || equipment.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        return equipmentRepository.save(equipment);
    }

    @Transactional(readOnly = true)
    public List<Equipment> listMyEquipments(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserId is missing.");
        }
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }
        return userEquipmentRepository.findByUser_Id(userId).stream()
                .map(UserEquipment::getEquipment)
                .toList();
    }

    @Transactional
    public List<Equipment> replaceMyEquipments(UUID userId, List<UUID> equipmentIds) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserId is missing.");
        }
        if (equipmentIds == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "equipmentIds is required.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        LinkedHashSet<UUID> uniqueIds = new LinkedHashSet<>();
        for (UUID id : equipmentIds) {
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Equipment id must not be null.");
            }
            uniqueIds.add(id);
        }

        List<UUID> requestedIds = List.copyOf(uniqueIds);
        List<Equipment> foundEquipments = equipmentRepository.findAllById(requestedIds);
        if (foundEquipments.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more equipment ids were not found.");
        }

        userEquipmentRepository.deleteByUser_Id(userId);
        List<UserEquipment> rows = foundEquipments.stream().map(equipment -> {
            UserEquipment row = new UserEquipment();
            row.setId(new UserEquipmentId(userId, equipment.getId()));
            row.setUser(user);
            row.setEquipment(equipment);
            return row;
        }).toList();
        userEquipmentRepository.saveAll(rows);

        Map<UUID, Equipment> byId = foundEquipments.stream()
                .collect(Collectors.toMap(Equipment::getId, e -> e));
        return requestedIds.stream().map(byId::get).toList();
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
