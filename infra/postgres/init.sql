-- Один контейнер Postgres, у каждого сервиса своя БД.
-- Сервис не лезет в чужую БД — только через HTTP API.
CREATE DATABASE auth_db;
CREATE DATABASE recp_db;
CREATE DATABASE user_db;
