package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.BrewMethods;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface BrewMethodsRepository extends JpaRepository<BrewMethods, UUID> {
}
