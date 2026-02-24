-- 1) New versioning tables

create table if not exists coffeenotes.coffee_beans (
                                                        id uuid primary key default gen_random_uuid(),
                                                        owner_id uuid not null,
                                                        name varchar(255) not null,
                                                        roaster varchar(255),
                                                        origin varchar(255),
                                                        process varchar(255),
                                                        notes text,
                                                        is_global boolean not null default false,
                                                        created_at timestamp not null default now(),
                                                        updated_at timestamp not null default now(),
                                                        deleted_at timestamp,
                                                        constraint fk_coffee_beans_owner_id_users
                                                            foreign key (owner_id)
                                                                references coffeenotes.users (id)
                                                                on delete no action
                                                                on update no action
);

create table if not exists coffeenotes.recipe_tracks (
                                                         id uuid primary key default gen_random_uuid(),
                                                         owner_id uuid not null,
                                                         bean_id uuid not null,
                                                         method_id uuid not null,
                                                         title varchar(255) not null,
                                                         is_global boolean not null default false,
                                                         created_at timestamp not null default now(),
                                                         updated_at timestamp not null default now(),
                                                         deleted_at timestamp,
                                                         constraint fk_recipe_tracks_owner_id_users
                                                             foreign key (owner_id)
                                                                 references coffeenotes.users (id)
                                                                 on delete no action
                                                                 on update no action,
                                                         constraint fk_recipe_tracks_bean_id_coffee_beans
                                                             foreign key (bean_id)
                                                                 references coffeenotes.coffee_beans (id)
                                                                 on delete no action
                                                                 on update no action,
                                                         constraint fk_recipe_tracks_method_id_brew_methods
                                                             foreign key (method_id)
                                                                 references coffeenotes.brew_methods (id)
                                                                 on delete no action
                                                                 on update no action
);

create table if not exists coffeenotes.recipe_versions (
                                                           id uuid primary key default gen_random_uuid(),
                                                           track_id uuid not null,
                                                           version_number int not null,
                                                           is_current boolean not null default false,
                                                           title varchar(255) not null,
                                                           coffee_amount varchar(50),
                                                           water_amount varchar(50),
                                                           grind_size varchar(100),
                                                           brew_time_seconds int,
                                                           water_temperature_celsius int,
                                                           rating int,
                                                           method_payload text not null default '{}',
                                                           created_at timestamp not null default now(),
                                                           updated_at timestamp not null default now(),
                                                           deleted_at timestamp,
                                                           constraint recipe_versions_rating_ck
                                                               check (rating is null or rating between 1 and 5),
                                                           constraint fk_recipe_versions_track_id_recipe_tracks
                                                               foreign key (track_id)
                                                                   references coffeenotes.recipe_tracks (id)
                                                                   on delete no action
                                                                   on update no action
);

create unique index if not exists recipe_versions_track_version_uk
    on coffeenotes.recipe_versions (track_id, version_number);

create unique index if not exists recipe_versions_one_current_uk
    on coffeenotes.recipe_versions (track_id)
    where is_current = true and deleted_at is null;

create index if not exists recipe_tracks_owner_id_idx
    on coffeenotes.recipe_tracks (owner_id);

create index if not exists recipe_tracks_bean_id_idx
    on coffeenotes.recipe_tracks (bean_id);

create index if not exists recipe_tracks_method_id_idx
    on coffeenotes.recipe_tracks (method_id);

create index if not exists recipe_versions_track_id_idx
    on coffeenotes.recipe_versions (track_id);

-- 2) Backfill from legacy recipes -> beans/tracks/versions

create temporary table recipe_migration_map as
select
    r.id as old_recipe_id,
    gen_random_uuid() as bean_id,
    gen_random_uuid() as track_id,
    gen_random_uuid() as version_id
from coffeenotes.recipes r;

insert into coffeenotes.coffee_beans (
    id, owner_id, name, roaster, origin, process, notes, is_global, created_at, updated_at, deleted_at
)
select
    m.bean_id,
    r.owner_id,
    coalesce(nullif(r.title, ''), 'Migrated Bean'),
    null,
    null,
    null,
    null,
    coalesce(r.is_global, false),
    r.created_at,
    r.updated_at,
    r.deleted_at
from coffeenotes.recipes r
         join recipe_migration_map m on m.old_recipe_id = r.id;

insert into coffeenotes.recipe_tracks (
    id, owner_id, bean_id, method_id, title, is_global, created_at, updated_at, deleted_at
)
select
    m.track_id,
    r.owner_id,
    m.bean_id,
    r.method_id,
    r.title,
    coalesce(r.is_global, false),
    r.created_at,
    r.updated_at,
    r.deleted_at
from coffeenotes.recipes r
         join recipe_migration_map m on m.old_recipe_id = r.id;

insert into coffeenotes.recipe_versions (
    id, track_id, version_number, is_current, title,
    coffee_amount, water_amount, grind_size, brew_time_seconds,
    water_temperature_celsius, rating, method_payload,
    created_at, updated_at, deleted_at
)
select
    m.version_id,
    m.track_id,
    1,
    true,
    r.title,
    r.coffee_amount,
    r.water_amount,
    r.grind_size,
    r.brew_time_seconds,
    r.water_temperature_celsius,
    r.rating,
    '{}',
    r.created_at,
    r.updated_at,
    r.deleted_at
from coffeenotes.recipes r
         join recipe_migration_map m on m.old_recipe_id = r.id;

-- 3) Transition columns on relation tables (keep legacy columns for compatibility)

alter table coffeenotes.recipe_water_pours
    add column if not exists recipe_version_id uuid;

alter table coffeenotes.recipe_equipment
    add column if not exists recipe_version_id uuid;

alter table coffeenotes.favorites
    add column if not exists recipe_track_id uuid;

update coffeenotes.recipe_water_pours p
set recipe_version_id = m.version_id
from recipe_migration_map m
where p.recipe_id = m.old_recipe_id;

update coffeenotes.recipe_equipment e
set recipe_version_id = m.version_id
from recipe_migration_map m
where e.recipe_id = m.old_recipe_id;

update coffeenotes.favorites f
set recipe_track_id = m.track_id
from recipe_migration_map m
where f.recipe_id = m.old_recipe_id;

alter table coffeenotes.recipe_water_pours
    add constraint fk_recipe_water_pours_recipe_version_id_recipe_versions
        foreign key (recipe_version_id)
            references coffeenotes.recipe_versions (id)
            on delete no action
            on update no action;

alter table coffeenotes.recipe_equipment
    add constraint fk_recipe_equipment_recipe_version_id_recipe_versions
        foreign key (recipe_version_id)
            references coffeenotes.recipe_versions (id)
            on delete no action
            on update no action;

alter table coffeenotes.favorites
    add constraint fk_favorites_recipe_track_id_recipe_tracks
        foreign key (recipe_track_id)
            references coffeenotes.recipe_tracks (id)
            on delete cascade
            on update no action;

-- 4) Transition invariants on new relation columns

create unique index if not exists recipe_water_pours_version_order_uk
    on coffeenotes.recipe_water_pours (recipe_version_id, order_index);

create unique index if not exists recipe_equipment_version_equipment_uk
    on coffeenotes.recipe_equipment (recipe_version_id, equipment_id);

create unique index if not exists favorites_user_track_uk
    on coffeenotes.favorites (user_id, recipe_track_id);

create index if not exists recipe_water_pours_recipe_version_id_idx
    on coffeenotes.recipe_water_pours (recipe_version_id);

create index if not exists recipe_equipment_recipe_version_id_idx
    on coffeenotes.recipe_equipment (recipe_version_id);

create index if not exists favorites_recipe_track_id_idx
    on coffeenotes.favorites (recipe_track_id);

-- Optional hardening in this migration (safe if backfill succeeded)
alter table coffeenotes.recipe_water_pours
    alter column recipe_version_id set not null;

alter table coffeenotes.recipe_equipment
    alter column recipe_version_id set not null;

alter table coffeenotes.favorites
    alter column recipe_track_id set not null;