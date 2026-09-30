# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Burgee is a self-hostable feature flag service: a Spring Boot 4 / Kotlin backend (`backend/`, Java 25, Postgres, Flyway) and an Angular 21 admin SPA (`frontend/`). They ship as one Docker image: the SPA is built into `src/main/resources/static` and served by the backend from the same origin.

## Commands

```bash
./dev.sh                                   # backend :8080 in in-memory storage mode + ng serve :4200 (proxies /api), reads .env
./dev.sh --postgres                        # same, but with Postgres (docker) as storage
docker compose up --build                  # full production-like image on :8080 (admin/admin)
```

Backend (run from `backend/`):

```bash
./mvnw test                                # unit tests (surefire, src/test)
./mvnw test -Dtest=FeatureFlagServiceTest  # single unit test class (append #method for one test)
./mvnw verify                              # unit + integration tests (failsafe, *IT, needs Docker for Testcontainers) — what CI runs
./mvnw verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=AdminFlagControllerIT
./mvnw spring-boot:run                     # needs Postgres on localhost:5432 (see dev.sh), or BURGEE_STORAGE=memory
```

Frontend (run from `frontend/`):

```bash
npm start                                  # ng serve
npm test -- --watch=false                  # vitest via @angular/build:unit-test (what CI runs)
npm test -- --watch=false --include src/app/core/flag.service.spec.ts
npm run build
```

End-to-end (run from `e2e/`, needs Docker; see README for the env vars):

```bash
npm run e2e                                # build the production image, start it on :18080 with a fresh Postgres, run Playwright, tear down
npx playwright test tests/login.spec.ts    # one spec file
E2E_NO_BUILD=1 E2E_KEEP_STACK=1 npm run e2e  # reuse the last image and leave the stack up for debugging
npm run typecheck
```

CI: `.github/workflows/pr-build.yml` runs backend `./mvnw verify` (unit + integration tests) and the frontend tests on pull requests to `main`. `.github/workflows/master-builder.yml` runs the same tests on pushes to `main`, then builds and pushes a multi-arch image to GHCR.

## Backend architecture

Hexagonal (ports & adapters) under `io.github.janverhoeckx.burgee`. Each bounded context (`flag`, `user`, `audit`) has the same shape:

- `domain/`: immutable data classes holding the invariants. Updates return a new instance via `copy`.
- `application/port/inbound/`: use case interfaces, often several small ones per file (`CreateFlagUseCase`, `ToggleFlagUseCase`, …). Controllers depend only on these.
- `application/port/outbound/`: SPI interfaces (`*RepositoryPort`, `PasswordHasher`, `ActorProvider`).
- `application/service/`: implements the inbound ports and is `@Transactional`.
- `adapter/inbound/web/`: controllers and DTOs. `adapter/outbound/persistence/`: a Spring Data JDBC `*Row` + `*JdbcRepository` + `*Mapper`, hidden behind a `*PersistenceAdapter` that implements the port. Nothing outside the adapter sees Row classes.

Cross-context links go through inbound ports: `FeatureFlagService` records audit entries by calling `RecordAuditEntryUseCase` inside the same transaction. The audit context resolves the acting user through its `ActorProvider` port (`SecurityContextActorProvider`). `RestExceptionHandler`/`ApiError` live in `flag/adapter/inbound/web` but serve every controller.

Cross-cutting packages:
- `security/`: `SecurityConfig` chooses the auth mode from `burgee.auth.method` (`basic` | `jwt`). Both modes are stateless and have CSRF disabled. In basic mode, `UserDetailsService` loads users from the DB through `FindUserBySubjectUseCase`. In jwt mode, the JWT authorities converter calls `ResolveOrProvisionUserUseCase`, which auto-creates unknown `sub`s with role `NEW`, so authorities always come from the DB role and never from token claims. URL rules: `/api/v1/flags/**` and `/api/auth/info` are public, `/api/admin/**` requires `ADMIN`, other `/api/auth/**` requires authentication, and any other `/api/**` is denied.
- `storage/`: `StorageConfig` chooses the storage mode from `burgee.storage` (`postgres` | `memory`). In memory mode it supplies an embedded H2 DataSource in PostgreSQL mode plus the Postgres JDBC dialect, and refuses to start when a database URL was set explicitly (ADR 0001). `/api/auth/info` reports the mode so the SPA can show its banner.
- `web/WebConfig`: SPA fallback that serves `static/index.html` for any unknown path that is not under `api/` or `actuator/`.
- The bootstrap admin comes from `user/adapter/inbound/bootstrap/BootstrapAdminRunner` (`BURGEE_ADMIN_USERNAME`/`PASSWORD`, or `BURGEE_ADMIN_SUBJECT` in jwt mode).

Schema changes go in a new Flyway migration `backend/src/main/resources/db/migration/V<n>__*.sql`. Never edit an existing one. Migrations must also run on H2 in PostgreSQL mode, which backs the in-memory storage mode (`storage/StorageConfig`, ADR 0001), so avoid Postgres-only features; `InMemoryStorageIT` catches violations.

Configuration lives in `application.yml`: env vars map to `burgee.*` properties, and the full table is in README.md.

## Backend testing conventions

- Unit tests use **MockK** + AssertJ, with a fixed `java.time.Clock` injected into services.
- Integration tests are in `backend/src/it/kotlin` (added as a test source by `build-helper-maven-plugin` and run by failsafe). They extend `AbstractIT`, which starts one shared `postgres:16` Testcontainer, activates the `integration-test` profile and sets `@TestConstructor(autowireMode = ALL)`. ITs for the in-memory storage mode extend `AbstractInMemoryIT` instead (no container). Inject beans as constructor `private val`s, not `@Autowired lateinit var`.
- Spring Boot 4 specifics: Jackson 3 (`tools.jackson.databind.ObjectMapper`), and `AutoConfigureMockMvc` from `org.springframework.boot.webmvc.test.autoconfigure`.
- ITs authenticate with `httpBasic("admin", "admin")`. The DB is never reset between tests, so generate unique flag keys (`"x-${System.nanoTime()}"`). Keys must match `^[a-z0-9][a-z0-9._-]*$`.
- The `/add-or-modify-integration-test` project skill covers the IT recipe in detail.

## Frontend

A standalone-component Angular app. `src/app/core/` holds the HTTP services, guards and the auth interceptor. `src/app/pages/` holds routed pages, with routes in `app.routes.ts`. `AuthService` asks `/api/auth/info` which mode the backend runs in. In basic mode it stores the Basic credentials in storage. In jwt mode it runs a PKCE login with `oidc-client-ts` using the OIDC config the backend returns. `auth.interceptor` adds the matching `Authorization` header. The API base URL is same-origin by default and can be overridden with `window.__burgeeConfig.apiBaseUrl`. Specs sit next to the files they test (`*.spec.ts`) and run on vitest + jsdom.

## E2E testing conventions

- Run the e2e suite when you touch the dashboard, security or web config.
- The DB lives for the whole run and tests run `fullyParallel`, so every test makes its own Key with `uniqueKey('prefix')` from `support/unique-key.ts`.
- Set up preconditions through the `adminApi` fixture (`adminApi.flags.create(...)`). Use the UI only for the behaviour under test.
- Page Objects live in `pages/`, hold locators and intent methods (`createFlag`, `toggle(key)`, `delete(key)`), contain no assertions, and are injected as fixtures from `fixtures.ts`.
- Locators use roles, labels and text only. Find a flag's row with `getByRole('row').filter({ hasText: key })`. No `data-testid`. When an element has no accessible name, add one to the markup (as with the `Toggle <key>` checkbox).
- The SPA keeps basic auth in sessionStorage, which `storageState` skips. The `setup` project (`tests/auth.setup.ts`) signs in once and saves that entry to `.auth/`, and the `context` fixture restores it with `addInitScript`. Tests start signed in; `test.use({ signedIn: false })` starts signed out (only the login journey).

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues on janverhoeckx/burgee, via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the default labels: needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
