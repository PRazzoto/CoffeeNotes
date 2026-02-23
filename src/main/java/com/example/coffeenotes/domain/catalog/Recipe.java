package com.example.coffeenotes.domain.catalog;

import com.example.coffeenotes.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="recipes", schema = "coffeenotes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Recipe {
    private @Id
    @GeneratedValue UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "method_id", nullable = false)
    private BrewMethods method;

    @Column(nullable = false)
    private String title;

    @Column(name = "coffee_amount")
    private String coffeeAmount;
    @Column(name = "water_amount")
    private String waterAmount;
    @Column(name = "grind_size")
    private String grindSize;

    @Column(name = "brew_time_seconds")
    private Integer brewTimeSeconds;
    @Column(name = "water_temperature_celsius")
    private Integer waterTemperatureCelsius;

    private Integer rating;

    @Column(name = "is_global", nullable = false)
    private boolean isGlobal;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
