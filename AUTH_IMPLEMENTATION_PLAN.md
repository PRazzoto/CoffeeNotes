# CoffeeNotes Auth and User Workflow Plan

This document is the implementation roadmap for production-grade authentication and user workflow in CoffeeNotes.

## 1. Decisions and Architecture (Lock First)

These decisions are locked for this project:

1. Authentication will be in-app (no external identity provider in this phase).
2. Use JWT access token + refresh token.
3. Access token transport: `Authorization: Bearer <token>`.
4. Refresh token transport: HttpOnly + Secure cookie.
5. API style: stateless.
6. Access token TTL: 15 minutes.
7. Refresh token TTL: 14 days.
8. Signing algorithm: `RS256`.
9. Refresh tokens must be stored hashed in DB (never plain token).
10. Refresh sessions are revoked on logout and password change.
11. Protected routes must always derive identity from token principal (never request `userId`).
12. JWT issuer and audience:
    - `iss = coffeenotes-api`
    - `aud = coffeenotes-web`

## 2. Database and Domain Changes

1. Keep `users.role` as enum string (`USER`, `ADMIN`) with `@Enumerated(EnumType.STRING)`.
2. Create refresh session table via Flyway (example name: `auth_refresh_sessions`) with:
   - `id` (uuid pk)
   - `user_id` (uuid fk -> users.id)
   - `token_hash` (text/varchar, unique)
   - `expires_at` (timestamp)
   - `revoked_at` (timestamp nullable)
   - `created_at` (timestamp)
   - optional metadata: `ip`, `user_agent`
3. Add index on `user_id`, `expires_at`, `revoked_at`.
4. Keep `ddl-auto=validate` in all non-local environments.

## 3. Package Structure

Create these packages/modules:

1. `config`:
   - `SecurityConfig`
2. `security`:
   - token generation/validation
   - key loading
   - authentication utilities
3. `feature/auth`:
   - auth controller, DTOs, service
4. `feature/user`:
   - user profile controller/service
5. `feature/auth/repository`:
   - refresh session repository

## 4. Security Configuration

1. Enable stateless security:
   - `SessionCreationPolicy.STATELESS`
2. Disable CSRF for the API and enforce strict origin policy for auth endpoints using refresh cookies.
3. Configure CORS with explicit frontend origins (no wildcard in prod).
4. Route rules:
   - public: `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`
   - authenticated: `/api/auth/logout` and all business API routes
5. Enable method security for role and ownership checks.

## 5. Key Management and JWT Settings

1. Use asymmetric keys in production:
   - private key for signing
   - public key for verification
2. Local development can use env-provided test keys; production keys must come from environment/secret manager.
3. Add token claims:
   - `sub` (user id)
   - `email`
   - `role`
   - `iat`, `exp`, `iss`, `aud`
4. Validate issuer and audience during token parsing.
5. Do not log full JWTs.

## 6. Password and Credential Policy

1. Hash with BCrypt (`PasswordEncoder`), cost around 12.
2. Never store plain password.
3. Enforce password policy:
   - minimum length
   - basic strength checks
4. Use generic login failure message (`Invalid credentials`) to avoid user enumeration.

## 7. Auth API (MVP)

Implement endpoints in this order:

1. `POST /api/auth/register`
   - validate payload
   - normalize email
   - ensure unique email
   - hash password
   - create user with default role `USER`
2. `POST /api/auth/login`
   - authenticate email/password
   - issue access token + refresh token
   - persist hashed refresh token session
   - set refresh token as HttpOnly + Secure cookie
3. `POST /api/auth/refresh`
   - read refresh cookie
   - validate refresh token session
   - rotate refresh token (invalidate old session, create new)
   - issue new access token
4. `POST /api/auth/logout`
   - revoke current refresh session
   - clear refresh cookie

## 8. User API

Implement:

1. `GET /api/users/me`
   - return authenticated user profile
2. `PATCH /api/users/me`
   - update safe profile fields (display name, maybe email)
3. `PATCH /api/users/me/password`
   - require current password
   - validate new password policy
   - revoke active refresh sessions after password change

Never expose `passwordHash` in responses.

## 9. Integrate Existing Features with Auth

1. Remove `userId` request parameters from protected endpoints (starting with Recipe).
2. Resolve user id from authenticated principal/JWT subject.
3. Keep ownership checks in service layer using authenticated id.

## 10. Error Contract

Use consistent API errors:

1. `400` invalid input
2. `401` unauthenticated/invalid token
3. `403` forbidden
4. `404` not found (use where appropriate to avoid resource leakage)
5. `409` conflict (email already registered)

## 11. Security Hardening

1. Add login and refresh rate limiting.
2. Add failed-login backoff/temporary lock.
3. Use HTTPS in production only.
4. Store secrets/keys outside source control.
5. Add audit logs for:
   - login success/failure
   - refresh
   - logout
   - password change

## 12. Testing Plan

### Unit tests
1. register service
2. login service
3. refresh token rotation
4. password change and session revocation

### Controller tests
1. auth endpoints status and validation
2. `/users/me` and password update flows

### Security tests
1. protected routes reject missing token
2. invalid token returns `401`
3. role restrictions where applicable

### Integration tests
1. register -> login -> call protected route
2. refresh -> old refresh invalidated
3. logout -> refresh rejected

## 13. Delivery Sequence (Execution Order)

1. Flyway migration for refresh sessions.
2. Security config baseline (stateless, route rules, csrf/cors).
3. Password encoder + user details/authentication setup.
4. JWT key loading and token service.
5. `register` and `login`.
6. `refresh` and `logout`.
7. `/users/me` and password change.
8. Migrate Recipe endpoints from `userId` param to principal.
9. Add full test coverage.
10. Final production checklist and docs update.

## 14. Production Checklist Before Go-Live

1. Keys/secrets from secure environment, not files in repo.
2. All protected endpoints require JWT.
3. No endpoint trusts client-provided user id for authorization.
4. Refresh revocation and rotation verified.
5. Rate limiting enabled.
6. Monitoring and audit logs active.
7. End-to-end auth tests passing.
