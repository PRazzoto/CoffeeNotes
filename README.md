# CoffeeNotes (Backend)

CoffeeNotes is a Spring Boot backend for a notes/recipes app focused on coffee brewing.

## Status

- Runs locally with PostgreSQL via Docker Compose
- Flyway migrations enabled
- First endpoint work in progress

## Tech Stack

- Java 17
- Spring Boot 4.x
- Maven
- PostgreSQL (Docker)
- Flyway (schema migrations)

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
- If startup fails with schema validation errors, it usually means:
  - a migration wasn’t applied, or
  - the entity mapping and the SQL schema don’t match

## API (WIP)

Current endpoints are being shaped. For now, check:

- `src/main/java/com/example/coffeenotes/api/controller`

## Project Notes (Blog)

### 2026-01-27

- Set up PostgreSQL via `docker-compose.yml`
- Enabled Flyway and started schema migration work
- Began mapping domain entities to DTOs for API responses

