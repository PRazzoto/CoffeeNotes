-- Complete the transition from legacy recipe_id relations to the
-- versioned/track-based relation columns introduced in V7.

-- recipe_water_pours now belongs to recipe_versions only
alter table coffeenotes.recipe_water_pours
    drop constraint if exists fk_recipe_water_pours_recipe_id_recipes;

alter table coffeenotes.recipe_water_pours
    drop constraint if exists recipe_water_pours_recipe_order_uk;

drop index if exists coffeenotes.recipe_water_pours_recipe_id_idx;

alter table coffeenotes.recipe_water_pours
    drop column if exists recipe_id;

-- recipe_equipment now belongs to recipe_versions only
alter table coffeenotes.recipe_equipment
    drop constraint if exists recipe_equipment_pk;

alter table coffeenotes.recipe_equipment
    drop constraint if exists fk_recipe_equipment_recipe_id_recipes;

alter table coffeenotes.recipe_equipment
    drop column if exists recipe_id;

drop index if exists coffeenotes.recipe_equipment_version_equipment_uk;

alter table coffeenotes.recipe_equipment
    add constraint recipe_equipment_pk primary key (recipe_version_id, equipment_id);

-- favorites now belongs to recipe_tracks only
alter table coffeenotes.favorites
    drop constraint if exists favorites_pk;

alter table coffeenotes.favorites
    drop constraint if exists fk_favorites_recipe_id_recipes;

alter table coffeenotes.favorites
    drop column if exists recipe_id;

drop index if exists coffeenotes.favorites_user_track_uk;

alter table coffeenotes.favorites
    add constraint favorites_pk primary key (user_id, recipe_track_id);
