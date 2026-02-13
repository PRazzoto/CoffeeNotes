-- V4: Core domain schema with UUID IDs
-- NOTE: This migration converts existing coffeenotes.equipment.id from bigint to uuid.

create extension if not exists pgcrypto;

-- Convert equipment.id from bigint -> uuid to keep ID strategy consistent.
alter table coffeenotes.equipment
    add column if not exists id_uuid uuid default gen_random_uuid();

update coffeenotes.equipment
set id_uuid = gen_random_uuid()
where id_uuid is null;

alter table coffeenotes.equipment
    alter column id_uuid set not null;

alter table coffeenotes.equipment
    drop constraint if exists equipment_pkey;

alter table coffeenotes.equipment
    rename column id to legacy_id;

alter table coffeenotes.equipment
    rename column id_uuid to id;

alter table coffeenotes.equipment
    add constraint equipment_pkey primary key (id);

alter table coffeenotes.equipment
    alter column id set default gen_random_uuid();

create unique index if not exists equipment_name_uk on coffeenotes.equipment (name);

-- Legacy bigint id and sequence are no longer needed.
alter table coffeenotes.equipment
    drop column if exists legacy_id;

drop sequence if exists coffeenotes.equipment_seq;

create table if not exists coffeenotes.users (
    id uuid primary key default gen_random_uuid(),
    email varchar(255) not null,
    password_hash varchar(255) not null,
    display_name varchar(255) not null,
    role varchar(50) not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint users_email_uk unique (email)
);

create table if not exists coffeenotes.brew_methods (
    id uuid primary key default gen_random_uuid(),
    name varchar(255) not null,
    constraint brew_methods_name_uk unique (name)
);

create table if not exists coffeenotes.recipes (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null,
    method_id uuid not null,
    title varchar(255) not null,
    coffee_amount varchar(50),
    water_amount varchar(50),
    grind_size varchar(100),
    brew_time_seconds int,
    water_temperature_celsius int,
    rating int,
    is_global boolean not null default false,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    deleted_at timestamp,
    constraint recipes_rating_ck check (rating is null or rating between 1 and 5),
    constraint fk_recipes_owner_id_users
        foreign key (owner_id)
            references coffeenotes.users (id)
            on delete no action
            on update no action,
    constraint fk_recipes_method_id_brew_methods
        foreign key (method_id)
            references coffeenotes.brew_methods (id)
            on delete no action
            on update no action
);

create table if not exists coffeenotes.recipe_water_pours (
    id uuid primary key default gen_random_uuid(),
    recipe_id uuid not null,
    water_amount_ml int not null,
    time varchar(20) not null,
    order_index int not null,
    constraint fk_recipe_water_pours_recipe_id_recipes
        foreign key (recipe_id)
            references coffeenotes.recipes (id)
            on delete cascade
            on update no action,
    constraint recipe_water_pours_recipe_order_uk unique (recipe_id, order_index)
);

create table if not exists coffeenotes.recipe_equipment (
    recipe_id uuid not null,
    equipment_id uuid not null,
    constraint recipe_equipment_pk primary key (recipe_id, equipment_id),
    constraint fk_recipe_equipment_recipe_id_recipes
        foreign key (recipe_id)
            references coffeenotes.recipes (id)
            on delete cascade
            on update no action,
    constraint fk_recipe_equipment_equipment_id_equipment
        foreign key (equipment_id)
            references coffeenotes.equipment (id)
            on delete no action
            on update no action
);

create table if not exists coffeenotes.favorites (
    user_id uuid not null,
    recipe_id uuid not null,
    constraint favorites_pk primary key (user_id, recipe_id),
    constraint fk_favorites_user_id_users
        foreign key (user_id)
            references coffeenotes.users (id)
            on delete cascade
            on update no action,
    constraint fk_favorites_recipe_id_recipes
        foreign key (recipe_id)
            references coffeenotes.recipes (id)
            on delete cascade
            on update no action
);

create table if not exists coffeenotes.media_assets (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null,
    type varchar(20) not null,
    storage_key text not null,
    size_bytes bigint,
    created_at timestamp not null default now(),
    constraint fk_media_assets_owner_id_users
        foreign key (owner_id)
            references coffeenotes.users (id)
            on delete no action
            on update no action
);

create index if not exists recipes_owner_id_idx on coffeenotes.recipes (owner_id);
create index if not exists recipes_method_id_idx on coffeenotes.recipes (method_id);
create index if not exists recipes_is_global_idx on coffeenotes.recipes (is_global);
create index if not exists recipes_created_at_idx on coffeenotes.recipes (created_at);
create index if not exists recipes_deleted_at_idx on coffeenotes.recipes (deleted_at);
create index if not exists recipe_water_pours_recipe_id_idx on coffeenotes.recipe_water_pours (recipe_id);
create index if not exists media_assets_owner_id_idx on coffeenotes.media_assets (owner_id);

