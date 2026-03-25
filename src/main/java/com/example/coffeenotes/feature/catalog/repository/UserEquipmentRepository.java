package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.UserEquipment;
import com.example.coffeenotes.domain.catalog.UserEquipmentId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserEquipmentRepository extends JpaRepository<UserEquipment, UserEquipmentId> {
    @EntityGraph(attributePaths = {"equipment"})
    List<UserEquipment> findByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);
}
