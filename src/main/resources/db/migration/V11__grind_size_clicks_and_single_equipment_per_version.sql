-- 1) Convert grind_size from text to integer clicks
alter table coffeenotes.recipe_versions
    add column if not exists grind_size_clicks integer;

update coffeenotes.recipe_versions
set grind_size_clicks = case
    when grind_size is not null and grind_size ~ '^\s*\d+\s*$'
        then trim(grind_size)::integer
    else null
end;

alter table coffeenotes.recipe_versions
    drop column if exists grind_size;

alter table coffeenotes.recipe_versions
    rename column grind_size_clicks to grind_size;

-- 2) Keep only one equipment row per recipe version
delete from coffeenotes.recipe_equipment re
using coffeenotes.recipe_equipment re_keep
where re.recipe_version_id = re_keep.recipe_version_id
  and re.equipment_id > re_keep.equipment_id;

create unique index if not exists recipe_equipment_one_per_version_uk
    on coffeenotes.recipe_equipment (recipe_version_id);
