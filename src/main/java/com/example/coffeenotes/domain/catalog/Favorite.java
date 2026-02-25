package com.example.coffeenotes.domain.catalog;

import com.example.coffeenotes.domain.catalog.recipe.RecipeTrack;
import com.example.coffeenotes.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "favorites", schema = "coffeenotes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Favorite{
    @EmbeddedId
    private FavoriteId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("recipeTrackId")
    @JoinColumn(name = "recipe_track_id", nullable = false)
    private RecipeTrack recipeTrack;
}
