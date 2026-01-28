create sequence if not exists coffeenotes.equipment_seq;

alter table coffeenotes.equipment
    alter column id set default nextval('coffeenotes.equipment_seq');
