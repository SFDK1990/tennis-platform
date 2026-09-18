# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This is the **code project root** (`TennisPlatformApp/`). The backend skeleton (Fase 0 of the roadmap) exists: a compilable, runnable Spring Boot app with no business logic yet. `frontend/` does not exist yet.

**Repo layout convention:** the narrative architecture documentation lives one level up, in `../Documentos/_arquitectura/` (a sibling folder, written in Spanish, holding only `.md` files) — it is **not** part of this code project. Non-narrative design artifacts that this project consumes — `openapi.yaml` — live here, at this project's root, alongside `backend/` and (later) `frontend/`. Keep this separation when adding new artifacts: cross-service specs and configs (`openapi.yaml`, `compose.yaml`, `.env.example`) go in `TennisPlatformApp/`, narrative docs go in `../Documentos/_arquitectura/`. Each service owns its own `Dockerfile` inside its folder (`backend/Dockerfile`), since that is also its build context — `compose.yaml` points at it with `build.context: ./backend`.

The docs are the source of truth for design decisions; treat them as binding requirements when implementing code, not just background reading (all paths below are relative to `../Documentos/_arquitectura/`):

- `00-indice-arquitectura.md` — index and consolidated decisions (including resolved product decisions, see below)
- `01-product-architect.md` — MVP scope, actors, business rules, entity states
- `02-arquitectura.md` / `02-software-architect.md` — technical architecture, module map, REST API, error model
- `03-backend-engineer-java-spring.md` — backend layering and rules detail
- `04-frontend-engineer-next-react.md` — frontend structure and conventions
- `05-database-engineer.md` — schema, constraints, indexes, Liquibase organization
- `06-devops-engineer.md` — repo layout, environments, CI expectations
- `07-qa-test-engineer.md` — test pyramid and critical test cases
- `08-security-engineer.md` — authn/authz, data protection, rate limiting
- `09-roadmap-implementacion.md` — phase-by-phase build order following the module dependency graph, with exit criteria per phase
- `10-diagrama-er.md` — ER diagram (Mermaid) and draft PostgreSQL DDL per Liquibase changelog group
- `11-contrato-api.md` — API contract conventions and business-code → HTTP status mapping; the actual OpenAPI spec is `openapi.yaml` at this project's root
- `12-metodologia-trabajo.md` — the agreed working process and phase status table; read it before starting anything
- `13-fase5-analisis-identity.md` — Fase 5 analysis (identity module)
- `14-fase5.1-integracion-continua.md` — Fase 5.1 decisions (CI pipeline, static analysis, dependency checking)
- `15-convenciones-de-codigo.md` — **binding conventions**: language (code, comments and commits in English; narrative docs in Spanish), comment style, test naming, branching and PR flow, commit rules, and how the quality tooling is used

## Backend commands

```
cd backend
mvn verify                # what CI runs: Spotless, tests, SpotBugs, JaCoCo
mvn test                  # tests only (integration tests auto-skip if Docker isn't available)
mvn spotless:apply        # fix what Spotless rejects
mvn clean package         # build the jar
mvn spring-boot:run       # run locally (defaults to the "dev" profile)
```

CI (`../.github/workflows/ci.yml`) runs `mvn verify` and then **fails the build if any test
skipped**, so the Fase 4 failure mode — green build, silently skipped integration tests —
cannot come back unnoticed. SpotBugs exclusions live in `backend/spotbugs-exclude.xml` and
each one must carry its justification.

From `TennisPlatformApp/`:

```
docker compose up postgres    # just the database, for running the backend from an IDE
docker compose up -d --build  # whole stack (postgres + backend)
docker compose down           # stop; add -v to also wipe the postgres volume
```

Two things that silently break the integration tests if changed carelessly:

- `pom.xml` pins `testcontainers.version` above the version managed by the Spring Boot BOM. Docker Engine 29 dropped support for old Docker API versions, and the BOM-managed docker-java still negotiates one of them — the symptom is not a failure but every Testcontainers test *skipping* while the build stays green.
- `AbstractIntegrationTest` uses the singleton-container pattern (started in a static block, never stopped, reaped by Ryuk). Switching it to `@Container` stops the database after the first test class, and every later class fails with `Failed to obtain JDBC Connection`.

Always check the `Skipped:` count in the surefire summary, not just `BUILD SUCCESS`.

`dev` profile connects to Postgres using `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` env vars, all defaulting to sane localhost values so `mvn spring-boot:run` works out of the box against a local Postgres (e.g. `docker compose up postgres` from this directory). `prod` profile requires those env vars explicitly (no defaults, fails fast if missing). See `backend/src/main/resources/application*.yml`.

## Product summary

Single-teacher tennis lesson booking platform (MVP). Roles: `ADMIN`, `TEACHER` (exactly one), `STUDENT`. Students self-register publicly but can only book once the teacher has taken them under management. Lessons are individual or group; bookings are automatic and immediately confirmed; cancellations are allowed up to 24 hours before with no penalties (except `ADMIN`, who can cancel anytime). Calendar times are shown in local time but persisted as UTC instants; students also see the teacher's timezone. There is a single physical court/location in the MVP — no court/resource entity is modeled. Explicitly out of scope for the MVP: payments, penalties, external notifications, external calendar sync, multiple teachers, multi-tenancy, Kafka/microservices, automated backups, formal auditing.

**Resolved product decisions** (previously open questions, now settled — see `00-indice-arquitectura.md` and `01-product-architect.md`): a student becomes "managed" when the teacher looks them up by email and links them explicitly; the teacher account is created via bootstrap (seed/migration), not public registration or an admin console action; production hosting provider is deliberately deferred — develop and test with Docker Compose until real users are onboarded; transactional email (verification, password reset) uses generic SMTP (`spring-boot-starter-mail`), not a proprietary provider.

## Target architecture (to build toward)

**Stack:** Next.js/React/TypeScript/Tailwind/PWA frontend → REST API → Java 21/Spring Boot/Spring Security backend → PostgreSQL, migrated exclusively via Liquibase.

**Backend style:** Modular Monolith — no microservices, no Kafka. Each module implements hexagonal architecture internally:

```
backend/<module>/
  domain/                    # entities, value objects, rules, exceptions — no Spring/JPA/REST deps
  application/port/in/       # use case interfaces
  application/port/out/      # repository/service interfaces (ports)
  application/service/       # use case implementations, transaction boundaries
  adapters/in/web/           # REST controllers, request/response DTOs
  adapters/out/persistence/  # JPA repositories/entities implementing the out ports
  configuration/             # Spring wiring
```

Modules: `identity`, `teacher`, `student`, `availability`, `lesson`, `booking`, `administration`, `calendar`, `shared`.

**Allowed module dependencies** (one-directional, no cycles):
- `identity` depends on nothing else
- `teacher` → `identity`
- `student` → `identity`, `teacher`
- `availability` → `teacher`
- `lesson` → `teacher`, `availability`
- `booking` → `student`, `lesson`
- `administration` → `identity`, `teacher`, `student`
- `calendar` → only public query interfaces of other modules (never writes to another module's domain)
- `shared` → nothing (technical primitives only: IDs, common errors, clock; no business logic)

Cross-module access must go through a module's public ports — never reach into another module's JPA entities, repositories, or internal adapters directly.

**This is enforced, not just documented.** `src/test/java/com/tennisplatform/architecture/ModuleBoundariesTest.java` holds 21 ArchUnit rules covering the dependency graph of all nine modules (including the six still empty), the hexagonal layers, cross-module access through `application/port/in` only, and `calendar` being read-only. They run inside `mvn test` — no profile, no tag, no Docker — so they cannot be skipped. `config`, `error` and `web` are not modules and stay outside the graph by decision: they are the composition root, the global error mapping and the correlation-id filter. Rationale and the four decisions behind the rules: `../Documentos/_arquitectura/17-analisis-archunit-limites-modulares.md`.

A practical consequence: to check a caller's role from another module, use `AuthenticatedUser.isTeacher()` rather than comparing against `identity.domain.Role` — the comparison imports identity's domain and the rules reject it.

**Frontend style:** feature-based folders (`authentication`, `profile`, `students`, `availability`, `calendar`, `lessons`, `bookings`, `administration`), not a global controllers/services soup. A single centralized HTTP client owns auth headers, session refresh, error normalization, and typed responses — components never call the API ad hoc. Backend is the single source of truth for lessons/bookings/availability state; the frontend must treat local calendar state as potentially stale and handle `409` conflicts on booking rather than trusting cached availability.

## Business rules that must hold regardless of layer

These are enforced primarily in the backend/database (frontend validation is UX-only, never authoritative):

- Lesson duration: minimum 30 minutes, must be a multiple of 30, cannot cross midnight.
- Individual lesson capacity is always 1; group lesson capacity is configurable and can start with a single participant.
- No exceeding lesson capacity; no duplicate bookings for the same student+lesson.
- No overlapping lessons for the teacher; no overlapping bookings for the same student.
- A student may only book if currently managed by the teacher.
- Cancellations (student or teacher) are only allowed ≥24 hours before the lesson start; no penalties in the MVP.
- Deactivating a student cancels their active future bookings.
- The teacher may create lessons outside configured availability only via an explicit override action.
- Booking the last available slot must be safe under concurrency — this requires transactional locking plus PostgreSQL constraints, not just application-level checks (see "last slot" race in `07-qa-test-engineer.md`).

Booking use case must, within one transaction: lock the lesson, check status/capacity, check for duplicate booking, check the student's schedule for overlaps, then create the booking.

## API and error conventions

REST is versioned under `/api/v1`. The full contract — all 24 MVP endpoints with request/response schemas — is defined in `openapi.yaml` at this project's root; `../Documentos/_arquitectura/11-contrato-api.md` documents the conventions around it (pagination, date format, auth). Errors use Problem Details (`application/problem+json`) with business codes: `LESSON_FULL`, `LESSON_OVERLAP`, `BOOKING_ALREADY_EXISTS`, `STUDENT_SCHEDULE_OVERLAP` → 409 (client should treat as stale calendar state and re-fetch); `STUDENT_NOT_MANAGED` → 403; `CANCELLATION_WINDOW_EXPIRED` → 422 (doesn't apply when the canceller is `ADMIN`). General mapping: 400 validation/format, 401 unauthenticated, 403 unauthorized, 404 not found, 409 conflict, 422 business rule violation, 500 unexpected. DTOs are always separate from domain/JPA entities — never expose JPA entities, password hashes, or full tokens over the wire.

## Security constraints

- Short-lived access tokens (`Authorization: Bearer`) + rotating refresh tokens carried in an `HttpOnly`/`Secure`/`SameSite` cookie — never in `localStorage` or the response body. Refresh tokens are stored hashed with a `family_id` per rotation chain so reuse of an already-rotated token can be detected and the whole family revoked.
- Passwords hashed with BCrypt or Argon2id.
- Authorization is always role **and** ownership/relationship based (e.g., a student can only see their own bookings, only the teacher can manage their own students) — hiding a UI control is never treated as authorization.
- Never trust client-supplied roles or ownership fields in DTOs.
- Rate limit auth-adjacent endpoints (login, register, email verification, password reset, refresh, admin endpoints).
- Never log passwords, tokens, full national ID numbers, or full addresses.
- `ADMIN` accounts cannot be created via public registration.

## Data model notes

PostgreSQL with UUID public identifiers (`gen_random_uuid()`, `pgcrypto`), UTC timestamps, IANA timezone strings stored as configuration data. Full diagram and draft DDL: `../Documentos/_arquitectura/10-diagrama-er.md`. Key tables: `users`, `teacher_profiles`, `student_profiles`, `teacher_students`, `email_verifications`, `password_reset_tokens`, `refresh_tokens`, `weekly_availability_rules`, `availability_exceptions`, `lessons`, `bookings`, `platform_configuration`. There is no separate `attendance` table — attendance is modeled as `ATTENDED`/`NO_SHOW` values on `bookings.status`.

Overlap/capacity/uniqueness invariants are backed by PostgreSQL constraints, not application logic alone: a partial unique index on `users` enforces the single-`TEACHER` invariant at the schema level; `EXCLUDE USING gist` constraints (requires the `btree_gist` extension) enforce no-overlap for both the teacher's lessons and a student's confirmed bookings — the latter requires denormalizing the lesson's `starts_at`/`ends_at` onto the `bookings` row at booking time, since exclusion constraints can't join across tables. Lesson capacity is still primarily protected by the transactional `SELECT ... FOR UPDATE` on `lessons` described in `02-arquitectura.md` §11, optionally backed by a trigger as a last-resort check. Liquibase changelogs are organized by module/context and are append-only — once executed, a changeset is never edited; fixes are new changesets.

## Testing expectations (once code exists)

Full pyramid: domain unit tests (no Spring), application tests against mocked ports, REST tests, integration tests against real PostgreSQL via Testcontainers, concurrency tests (especially "last slot" booking races and teacher/student overlap checks), ArchUnit tests for module boundary and hexagonal-layer enforcement, Liquibase migration tests (must run cleanly from an empty database), and Playwright E2E covering registration → verification → login → student management → availability → booking → cancellation → attendance → administration flows.

## Implementation roadmap

`../Documentos/_arquitectura/09-roadmap-implementacion.md` defines the build order (Phase 0 scaffold → identity → teacher/student → availability → lesson → booking → calendar → administration → pre-launch hardening), each with an explicit exit criterion. Don't start a module's phase before its dependencies (per the module dependency graph above) are already operational — this is what the roadmap sequences.
