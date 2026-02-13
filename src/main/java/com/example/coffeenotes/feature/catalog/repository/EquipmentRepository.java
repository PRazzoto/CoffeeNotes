package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {
}
