create table if not exists coffeenotes.auth_refresh_sessions(
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    token_hash varchar(255) not null,
    expires_at timestamp not null,
    revoked_at timestamp,
    created_at timestamp not null default now(),
    ip varchar(45),
    user_agent varchar(512),
    constraint fk_auth_refresh_sessions_user_id_users
        foreign key (user_id)
            references coffeenotes.users (id)
            on delete cascade
            on update no action
);

create unique index if not exists auth_refresh_sessions_token_hash_uk
    on coffeenotes.auth_refresh_sessions (token_hash);

create index if not exists auth_refresh_sessions_user_id_idx
    on coffeenotes.auth_refresh_sessions (user_id);

create index if not exists auth_refresh_sessions_expires_at_idx
    on coffeenotes.auth_refresh_sessions (expires_at);

create index if not exists auth_refresh_sessions_revoked_at_idx
    on coffeenotes.auth_refresh_sessions (revoked_at);