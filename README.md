# CoffeeNotes (Backend)

CoffeeNotes is a Spring Boot backend for a notes/recipes app focused on coffee brewing.

## Status

- Runs locally with PostgreSQL via Docker Compose
- Flyway migrations enabled (Spring Boot 4 + `spring-boot-flyway`)
- Equipment CRUD endpoints implemented
- Recipe CRUD endpoints implemented (with soft delete)
- Recipe endpoints now use authenticated JWT subject instead of `userId` query param
- User profile/account endpoints implemented (`get`, `update display name`, `change password`, `delete account`)
- Versioned recipe data-model foundation added (`coffee_beans`, `recipe_tracks`, `recipe_versions`)
- Auth register/login flow implemented with JWT access tokens
- Auth refresh/logout flow implemented with HttpOnly refresh-token cookies
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
  - `V5`: description columns updated to `VARCHAR(255)`
  - `V6`: auth refresh sessions table (`auth_refresh_sessions`)
  - `V7`: versioned recipe foundation and transition columns (`coffee_beans`, `recipe_tracks`, `recipe_versions`, relation backfill to version/track IDs)

## API (WIP)

Current endpoints:

- `GET /api/equipment/listAll`
- `POST /api/equipment/createEquipment`
- `PUT /api/equipment/editEquipment/{id}`
- `DELETE /api/equipment/deleteEquipment/{id}`
- `GET /api/recipe/getRecipes`
- `POST /api/recipe/createRecipe`
- `PATCH /api/recipe/updateRecipe/{id}`
- `DELETE /api/recipe/deleteRecipe/{id}`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/user/getUser`
- `PATCH /api/user/updateUser`
- `PATCH /api/user/updatePassword`
- `DELETE /api/user/deleteUser`

Notes:
- `{id}` is UUID for update/delete routes.
- Equipment DTO responses currently expose `name` and `description`.
- Recipe endpoints resolve user ownership from JWT `sub` claim.
- User endpoints resolve account identity from JWT `sub` claim.
- Access token claims include `sub`, `email`, `role`, `iss`, `aud`, `iat`, `exp`.

## Auth & Security (Current State)

### Implemented

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/user/getUser`
- `PATCH /api/user/updateUser`
- `PATCH /api/user/updatePassword`
- `DELETE /api/user/deleteUser`
- Stateless security with JWT (`SessionCreationPolicy.STATELESS`)
- Password hashing with `BCryptPasswordEncoder(12)`
- JWT signing/validation with RSA keys (private/public PEM)
- Access token TTL: `900` seconds (15 minutes)
- Refresh token TTL: `14` days
- Refresh token stored in HttpOnly cookie (`refresh_token`)
- Password change revokes active refresh sessions for the current user
- Password change and account deletion clear refresh-token cookie in response

### Register Rules

- Required fields: `email`, `password`, `displayName`
- Duplicate email returns `409 CONFLICT`
- Email is normalized (`trim + lowercase`) before persistence
- Password is validated and then hashed

### Login Rules

- Required fields: `email`, `password`
- Credentials validated through `AuthenticationManager`
- Invalid credentials return `401 UNAUTHORIZED`
- Success returns `accessToken`, `tokenType` (`Bearer`), and `expiresIn`

### Route Protection

- Public routes in security config:
  - `/api/auth/register`
  - `/api/auth/login`
  - `/api/auth/refresh`
  - `/api/auth/logout`
- All other routes require a valid JWT

### Testing

- Windows: `.\mvnw.cmd test`
- macOS/Linux: `./mvnw test`

### Insomnia Tip

If `POST /api/auth/register` unexpectedly returns `401`, check:

- request-level auth is `No Auth`
- workspace/collection-level auth is `No Auth`
- there is no `Authorization` header inherited by the request

For implementation details, check:

- `src/main/java/com/example/coffeenotes/api/controller`
- `src/main/java/com/example/coffeenotes/feature/catalog/service`
- `src/main/java/com/example/coffeenotes/feature/user/service`
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

### 2026-02-14

- Implemented Recipe CRUD controller/service flow with DTOs (`create`, `list`, `update`, soft `delete`)
- Added ownership checks and soft-delete handling in `RecipeService`
- Added Recipe tests: `RecipeServiceTest` and `RecipeControllerTest`

### 2026-02-16

- Added auth foundation: `Role` enum, `CustomUserDetailsService`, JWT token service, security configuration
- Added JWT key configuration with issuer/audience validation in decoder
- Added auth DTOs and service/controller for register and login flows
- Added migration `V6__auth_refresh_sessions.sql` and refresh session persistence model

### 2026-02-17

- Added `POST /api/auth/refresh` to rotate refresh token and issue a new access token
- Added `POST /api/auth/logout` to revoke active refresh token and clear the cookie
- Added `RefreshTokenService` and auth flow tests for refresh/logout behavior
- Updated Recipe controller and security tests to use authenticated JWT subject (`sub`) instead of passing `userId` query params

### 2026-02-23

- Added user account endpoints for current user profile retrieval, display-name update, password change, and account deletion
- Added password-change revocation flow for active refresh sessions
- Added `UserControllerTest` and `UserServiceTest` coverage for user endpoint/service behavior

### 2026-02-24

- Added migration `V7__refactor_of_database.sql` with versioned recipe foundation (`coffee_beans`, `recipe_tracks`, `recipe_versions`)
- Backfilled legacy `recipes` data into track/version records and added transition FK columns for relation tables
- Added new domain entities: `CoffeeBean`, `RecipeTrack`, `RecipeVersion`
- Added repositories for versioned model groundwork: `CoffeeBeanRepository`, `RecipeTrackRepository`, `RecipeVersionRepository`
