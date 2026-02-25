package com.example.coffeenotes.domain.catalog.recipe;

import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
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
@Table(name = "recipe_tracks", schema = "coffeenotes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecipeTrack {

    private @Id
    @GeneratedValue UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bean_id", nullable = false)
    private CoffeeBean bean;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "method_id", nullable = false)
    private BrewMethods method;

    @Column(nullable = false, length = 255)
    private String title;

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
