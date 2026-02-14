package com.example.coffeenotes.domain.catalog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "equipment", schema = "coffeenotes")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    private @Id
    @GeneratedValue UUID id;

    @Column(nullable=false)
    private String name;
    private String description;

}

