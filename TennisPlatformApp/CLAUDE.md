# CLAUDE.md

Guidance for Claude Code in `TennisPlatformApp/`, the code project. The working process, the
phase status and the traps are in `../CLAUDE.md`; the design documents are in
`../Documentos/_arquitectura/` (Spanish), indexed by `00-indice-arquitectura.md`. They are
binding, not background reading.

`openapi.yaml` must describe what the backend does, not what it will do.

## Commands

```
cd backend
mvn verify           # what CI runs; check the Skipped count, not only BUILD SUCCESS
mvn spotless:apply
mvn spring-boot:run  # dev profile; needs `docker compose up postgres`
```

## Backend architecture

Modular monolith. Each module is hexagonal: `domain/` (no Spring, JPA or REST),
`application/port/in` (use cases), `application/port/out` (what it needs from adapters),
`application/port/spi` (what it needs from *another module*, see below), `application/service`
(use cases and transaction boundaries), `adapters/in/web`, `adapters/out/persistence`,
`configuration/` (manual wiring).

Allowed dependencies, one-directional:

| Module | May depend on |
|---|---|
| `identity`, `platform`, `shared` | nothing |
| `teacher` | `identity` |
| `student` | `identity`, `teacher`, `platform` |
| `availability` | `teacher`, `identity` |
| `lesson` | `teacher`, `availability`, `identity`, `platform` |
| `booking` | `student`, `lesson`, `identity` |
| `administration` | `identity`, `teacher`, `student`, `platform` |
| `calendar` | query ports only (`Get*`, `Find*`, `Query*`); it never writes |

Every module may also use `shared.domain`: technical primitives only (`DateRange`,
`ResultPage`, `ForbiddenOperationException`), no business rules. `config`, `error`,
`observability` and `web` are not modules; `web` may only call inbound ports.

Across modules, only `application/port/in` is visible, plus one exception: **a module may
implement another module's `application/port/spi`, never call it**. That is how `lesson` and
`student` get bookings counted and cancelled without depending on `booking`: they declare
`LessonBookings` / `StudentBookings`, and `booking` implements them, inside the caller's
transaction.

All of this is enforced by `ModuleBoundariesTest` (ArchUnit, runs in `mvn test`, cannot be
skipped). `PublicPortsTest` proves the cross-module rule rejects what it must. Rationale:
`17-analisis-archunit-limites-modulares.md`.

To check a role from another module, use `AuthenticatedUser.isTeacher()` / `isStudent()` /
`isAdmin()`; comparing against identity's `Role` imports its domain and the rules reject it.
Views carry wire values (strings), not domain enums, for the same reason.

## Frontend architecture

It mirrors the backend: `src/modules/<module>` (`api.ts` with the TanStack Query hooks, plus
`components/`), `src/shared` (the API client, session, generated types, time helpers, UI
primitives) and `src/app` (routes; the composition root, the only place that combines modules).
A module imports only `@/shared` and itself, never another module or a parent path: enforced by
`eslint.config.mjs`. Every call goes through `shared/api/client.ts` (openapi-fetch typed from
`openapi.yaml`); the access token lives only in memory, and the refresh cookie restores it.
After any mutation every query is invalidated (`shared/api/query.ts`). Dates are shown in the
teacher's zone. Decisions: `22-fase11-analisis-frontend.md`. Read `frontend/AGENTS.md` first:
Next 16 differs from what you may remember.

## API and errors

REST under `/api/v1`, Problem Details with a `code`. `409` means the client's view is stale and
re-reading may change the answer; `422` is a business rule re-reading will not change; `403` is
role or relationship; somebody else's resource answers `404`. Module errors are mapped in each
module's `@RestControllerAdvice` with `Problems.of`; the shared ones (`ForbiddenOperationException`,
`DateRange`, malformed requests) in `error/GlobalExceptionHandler`. Codes and conventions:
`11-contrato-api.md`.

Auth: short access token as `Authorization: Bearer`; rotating refresh token in an `HttpOnly`
cookie; CSRF only on `/auth/refresh` and `/auth/logout` (`X-XSRF-TOKEN` echoing the `XSRF-TOKEN`
cookie). Security rules: `02-arquitectura.md` §13.

## Logs and metrics

Every line carries `correlationId` and, once authenticated, `userId` and `role` (MDC). The
browser sends the id (`X-Correlation-Id`), and a 500 shows its first 8 characters to the user:
search the log for them. `prod` writes JSON (ECS), `dev` and `test` text. Each API request leaves
one line (`POST /api/v1/lessons/{id}/bookings 409 LESSON_FULL 12 ms`), and each change of state
one line with ids only. Never log an email, name, phone, password, token or IP: `RequestLogTest`
fails if the account and booking flows do. A 500 is logged with the type and frames of each
exception but no messages, which can quote database values (`UnexpectedErrorTest`). `http.server.requests` is tagged with the error
`code` (`none` on success); `/actuator/metrics` is for `ADMIN` only. Decisions:
`28-fase16-analisis-observabilidad.md`.

## Data

PostgreSQL, UUID ids, instants as `timestamptz` in UTC, Liquibase changelogs per module and
append-only. Invariants are backed by the schema, not only by the application: the single
teacher (partial unique index), no overlapping lessons and no overlapping bookings of a student
(`EXCLUDE USING gist`), one confirmed booking per student and lesson (partial unique index). The
last seat is protected by `SELECT ... FOR UPDATE` on the lesson. DDL: `10-diagrama-er.md`.

## Tests

Domain tests without Spring, application tests with mocked ports, API and integration tests
against real PostgreSQL through Testcontainers (`AbstractIntegrationTest`), concurrency tests
for the last-seat race, ArchUnit for the boundaries, and the contract tests in
`src/test/java/com/tennisplatform/contract`: every integration-test response is validated
against `openapi.yaml`, the routes of the code and of the spec must be the same set, and the
common statuses (401, 429, 400) must be documented on every operation they apply to. `AccessMatrixTest`
calls every operation with six identities; a new operation needs its row there. Test names are sentences
(`twoStudentsRaceForTheLastSeatAndExactlyOneWins`). `mvn verify` fails below 97 % of lines or
83 % of branches.

The frontend has Vitest unit tests (`src/**/*.test.ts`) and the Playwright flows in `frontend/e2e`
(Spanish names, one sentence each), which drive the screens by role and accessible name. What a
test needs beforehand is made through the API (`e2e/support/arrange.ts`) and undone afterwards;
emails are read from Mailpit.
