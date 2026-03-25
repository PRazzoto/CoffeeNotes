create table if not exists coffeenotes.user_equipment (
    user_id uuid not null,
    equipment_id uuid not null,
    created_at timestamp not null default now(),
    constraint user_equipment_pk primary key (user_id, equipment_id),
    constraint fk_user_equipment_user_id_users
        foreign key (user_id)
            references coffeenotes.users (id)
            on delete cascade
            on update no action,
    constraint fk_user_equipment_equipment_id_equipment
        foreign key (equipment_id)
            references coffeenotes.equipment (id)
            on delete cascade
            on update no action
);

create index if not exists user_equipment_user_id_idx
    on coffeenotes.user_equipment (user_id);

create index if not exists user_equipment_equipment_id_idx
    on coffeenotes.user_equipment (equipment_id);
