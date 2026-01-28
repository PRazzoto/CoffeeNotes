create schema if not exists coffeenotes;

create sequence if not exists coffeenotes.equipment_seq;

create table coffeenotes.equipment (
                                       id bigint primary key default nextval('coffeenotes.equipment_seq'),
                                       name varchar(255) not null,
                                       description text
);