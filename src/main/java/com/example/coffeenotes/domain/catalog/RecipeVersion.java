package com.example.coffeenotes.domain.catalog;

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
@Table(name = "recipe_versions", schema = "coffeenotes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecipeVersion {
    private @Id
    @GeneratedValue UUID id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private RecipeTrack track;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "coffee_amount", length = 50)
    private String coffeeAmount;
    @Column(name = "water_amount", length = 50)
    private String waterAmount;
    @Column(name = "grind_size", length = 100)
    private String grindSize;

    @Column(name = "brew_time_seconds")
    private Integer brewTimeSeconds;
    @Column(name = "water_temperature_celsius")
    private Integer waterTemperatureCelsius;

    private Integer rating;

    @Column(name = "method_payload", nullable = false)
    private String methodPayload;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
