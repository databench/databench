DROP SCHEMA IF EXISTS databenchdev CASCADE;
CREATE SCHEMA databenchdev AUTHORIZATION postgres;
SET search_path TO databenchdev;
ALTER USER postgres SET search_path to databenchdev,public;

create table SQLTYPED_ACCOUNT(
  ID int NOT NULL,
  TRANSFERS varchar NOT NULL, 
  BALANCE int NOT NULL, 
  PRIMARY KEY (id)
);
