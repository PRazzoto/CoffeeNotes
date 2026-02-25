package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.CoffeeBean;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, UUID> {
    Optional<CoffeeBean>
    findByIdAndOwner_IdAndDeletedAtIsNull(UUID id, UUID ownerId);

    List<CoffeeBean>
    findAllByOwner_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID ownerId);

    List<CoffeeBean>
    findAllByOwner_IdAndNameContainingIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(UUID ownerId, String name);
}