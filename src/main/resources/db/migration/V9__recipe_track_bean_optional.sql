-- Allow creating recipe tracks without linking a specific bean.
alter table coffeenotes.recipe_tracks
    alter column bean_id drop not null;
