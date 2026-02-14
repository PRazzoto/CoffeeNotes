alter table coffeenotes.equipment
    add column if not exists description varchar(255);

alter table coffeenotes.brew_methods
    add column if not exists description varchar(255);
