package com.example.coffeenotes.domain.catalog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "brew_methods", schema = "coffeenotes")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BrewMethods {
    private @Id
    @GeneratedValue UUID id;

    @Column(nullable=false)
    private String name;
    private String description;
}
