package com.example.coffeenotes.feature.catalog.repository;

import com.example.coffeenotes.domain.catalog.CoffeeBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, UUID> {
    @Query("""
        SELECT b
        FROM CoffeeBean b
        WHERE b.deletedAt IS NULL
          AND (b.owner.id = :userId OR b.isGlobal = true)
        ORDER BY b.createdAt DESC
    """)
    List<CoffeeBean> findVisibleBeans(@Param("userId") UUID userId);
    List<CoffeeBean>
    findAllByOwner_Id(UUID ownerId);
}