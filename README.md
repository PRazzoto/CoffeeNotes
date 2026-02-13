# CoffeeNotes (Backend)

CoffeeNotes is a Spring Boot backend for a notes/recipes app focused on coffee brewing.

## Status

- Runs locally with PostgreSQL via Docker Compose
- Flyway migrations enabled (Spring Boot 4 + `spring-boot-flyway`)
- Equipment CRUD endpoints implemented
- Equipment IDs migrated to UUID
- Controller and service tests updated for UUID flow

## Tech Stack

- Java 17
- Spring Boot 4.x
- Maven
- PostgreSQL 16 (Docker)
- Flyway 11 (schema migrations)

## Local Setup

### 1) Start PostgreSQL

From the repo root:

```bash
docker compose up -d
```

### 2) Configure Java

Make sure `JAVA_HOME` points to a JDK 17 install and that `JAVA_HOME/bin` is on your `PATH`.

Quick check:

```bash
java -version
```

### 3) Run the server

```bash
./mvnw spring-boot:run
```

The server runs on `http://localhost:8080` by default.

## Database & Migrations

- Migrations live in `src/main/resources/db/migration`
- Flyway runs automatically on startup
- Current migration set:
  - `V1`..`V3`: initial equipment setup
  - `V4`: core domain schema (`users`, `brew_methods`, `recipes`, `recipe_water_pours`, `recipe_equipment`, `favorites`, `media_assets`) and UUID strategy alignment

## API (WIP)

Current endpoints:

- `GET /api/equipment/listAll`
- `POST /api/equipment/createEquipment`
- `PUT /api/equipment/editEquipment/{id}`
- `DELETE /api/equipment/deleteEquipment/{id}`

Notes:
- `{id}` is UUID for update/delete routes.
- Equipment DTO responses currently expose `name` and `description`.

For implementation details, check:

- `src/main/java/com/example/coffeenotes/api/controller`
- `src/main/java/com/example/coffeenotes/feature/catalog/service`
- `src/test/java/com/example/coffeenotes`

## Project Notes (Blog)

### 2026-01-27

- Set up PostgreSQL via `docker-compose.yml`
- Enabled Flyway and started schema migration work
- Began mapping domain entities to DTOs for API responses

### 2026-02-10

- Implemented full Equipment CRUD controller flow
- Added validation and error handling in `EquipmentService`
- Added controller tests (`@WebMvcTest`) for Equipment endpoints
- Added service unit tests for add, update, and delete scenarios

### 2026-02-13

- Added PostgreSQL datasource configuration in `application.yml`
- Added Flyway dependencies and Spring Boot 4 Flyway auto-configuration module
- Added `V4__core_domain_schema.sql` with full domain schema
- Migrated Equipment entity/repository/service/controller IDs to UUID
- Updated Equipment controller/service tests to UUID
