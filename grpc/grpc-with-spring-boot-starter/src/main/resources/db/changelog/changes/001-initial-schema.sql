--liquibase formatted sql
--changeset author:id
-- https://docs.liquibase.com/concepts/changelogs/sql-format.html

create table example
(
    id          uuid         not null primary key,
    name        varchar(150) not null unique,
    description text
);

--rollback drop table example;
