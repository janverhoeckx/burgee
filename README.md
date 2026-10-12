# Burgee

A simple, self-hostable, open-source feature flag service.

- **Stateless backend** — Spring Boot 4 + Kotlin + Spring Data JDBC + Postgres, scales horizontally behind any load balancer.
- **Hexagonal architecture** — domain, ports, and adapters cleanly separated; swapping persistence or transport requires no changes to the core.
- **Admin dashboard** — Angular 21 SPA, built into the backend image and served from the same origin.
- **User management** — create, edit, and delete users from the dashboard. Role-based access control with three roles: Admin, User, and New.
- **Flexible authentication** — HTTP Basic (default) or stateless JWT against any OIDC provider. Switch with a single environment variable.
- **Auto-provisioning** — SSO users are automatically created on first login with the `NEW` role; an admin upgrades them.
- **Public REST API** — evaluate flags from your apps with a single POST, optionally against an Evaluation Context (targeting).
- **Single container** — frontend and backend ship together; one image, one port.

## Quickstart

Try Burgee without a database, using the in-memory storage mode:

```bash
docker run --rm -p 8080:8080 -e BURGEE_STORAGE=memory ghcr.io/janverhoeckx/burgee:latest
```

Then open:

- **Dashboard**: http://localhost:8080 (default login: `admin` / `admin`)
- **Public flags API**: `POST http://localhost:8080/api/v1/flags/evaluate` (see [REST API](#rest-api))

> **In-memory mode is for evaluation and local development only.** All flags, users and audit entries are lost when the container stops, and the dashboard shows a banner saying so.

## Running for real

For a durable setup, run Burgee against Postgres (the default storage mode):

```bash
docker compose up --build
```

This starts Postgres and Burgee on http://localhost:8080 with the same default login.

> **Change the default admin credentials** before exposing Burgee anywhere. Set `BURGEE_ADMIN_USERNAME` and `BURGEE_ADMIN_PASSWORD` in your environment or a `.env` file. For JWT, set `BURGEE_AUTH_METHOD` and `BURGEE_ADMIN_SUBJECT` instead.

## REST API

### Public (no auth)

Clients read flags only through Evaluations: they submit an Evaluation Context of string Attributes and get the result back.

```
POST /api/v1/flags/{key}/evaluate   { "attributes": { "organisationId": "acme" } }
                                    → { "key": "checkout-v2", "enabled": true }      (404 if the key is unknown)
POST /api/v1/flags/evaluate         { "attributes": { "organisationId": "acme" } }
                                    → [{ "key": "checkout-v2", "enabled": true }, …] (every flag, disabled ones as false)
```

- `enabled` in the response is the Evaluation result, not the flag's master switch.
- The body is optional: a missing body or missing `attributes` counts as an empty context. Attributes no flag uses are ignored.
- `400 Bad Request` when an attribute value is not a JSON string (numbers, booleans, `null`, arrays and objects are rejected), when there are more than 50 attributes, when a name doesn't match `^[A-Za-z][A-Za-z0-9_.-]*$` or is longer than 64 characters, or when a value is blank or longer than 256 characters.

```bash
curl -X POST http://localhost:8080/api/v1/flags/checkout-v2/evaluate \
  -H 'Content-Type: application/json' -d '{"attributes":{"organisationId":"acme"}}'
```

The old `GET /api/v1/flags` and `GET /api/v1/flags/{key}` endpoints have been removed.

### Admin — Flags (requires `ADMIN` role)

```
GET    /api/admin/flags
GET    /api/admin/flags/{id}
POST   /api/admin/flags                 { "key": "...", "name": "...", "description": "...", "enabled": false, "conditions": [...] }
PUT    /api/admin/flags/{id}            { "name": "...", "description": "...", "enabled": true, "conditions": [...] }
POST   /api/admin/flags/{id}/toggle
DELETE /api/admin/flags/{id}
```

#### Targeting Rules

A flag's Targeting Rule is its list of **Conditions**. An Evaluation is `enabled AND every Condition matches`, so `enabled` stays the kill switch and an enabled flag without Conditions is on for everyone.

```json
"conditions": [
  { "attribute": "organisationId", "operator": "IN", "values": ["acme", "globex"] },
  { "attribute": "country",        "operator": "IN", "values": ["nl"] }
]
```

- A Condition matches when the Evaluation Context holds its `attribute` with one of its `values`. Matching is exact and case-sensitive. If the Attribute is missing from the context, the Condition doesn't match and the flag evaluates to false.
- `IN` is the only operator.
- `conditions` is optional on create and update and defaults to `[]`. Every save replaces the whole list, so a `PUT` without `conditions` removes them. Toggling keeps them.
- Admin responses include `conditions`. The public evaluate API never returns them.
- Duplicate values are silently removed.
- `400 Bad Request`, with the offending path in `fieldErrors` (e.g. `conditions[0].values[2]`), when:
  - an `attribute` doesn't match `^[A-Za-z][A-Za-z0-9_.-]*$` or is longer than 64 characters,
  - two Conditions use the same `attribute`,
  - `operator` is not `IN`,
  - `values` is empty or holds more than 1000 values (counted as submitted, before duplicates are removed),
  - a value is blank or longer than 256 characters.

> **Value lists are not secret.** Anyone who can reach the public evaluate API can find out whether a given value is in a Condition's list by submitting it and watching the result. Only put identifiers in value lists that you don't mind being guessed, never secrets such as tokens or passwords.

### Admin — Users (requires `ADMIN` role)

```
GET    /api/admin/users
GET    /api/admin/users/{id}
POST   /api/admin/users                 { "subject": "...", "email": "...", "displayName": "...", "role": "USER", "password": "..." }
PUT    /api/admin/users/{id}            { "email": "...", "displayName": "...", "role": "ADMIN", "password": "..." }
DELETE /api/admin/users/{id}
```

### Auth

```
GET    /api/auth/info                   → { "method": "basic", "oidc": null, "storage": "postgres" }  (public)
GET    /api/auth/user                   → { "name": "admin", "role": "ADMIN", "isAdmin": true }     (authenticated)
```

Example:

```bash
curl -u admin:admin -X POST http://localhost:8080/api/admin/flags \
  -H 'Content-Type: application/json' \
  -d '{"key":"new-checkout","name":"New checkout","enabled":true,
       "conditions":[{"attribute":"organisationId","operator":"IN","values":["acme"]}]}'
```

## Authentication

Burgee supports three authentication methods, selected via `BURGEE_AUTH_METHOD`:

### HTTP Basic (default)

Stateless, no external identity provider needed. Users are stored in the database with bcrypt-hashed passwords. A bootstrap admin is created on startup from `BURGEE_ADMIN_USERNAME` / `BURGEE_ADMIN_PASSWORD`.

### JWT / OIDC (stateless)

Stateless bearer-token validation that works with **any** OIDC provider (Keycloak, Auth0, Okta, Entra ID, …). The SPA performs the authorization-code (PKCE) login client-side via [`oidc-client-ts`](https://github.com/authts/oidc-client-ts) and sends the access token as a bearer; the backend validates it against the issuer's published keys. Set `BURGEE_JWT_ISSUER_URI`, `BURGEE_JWT_CLIENT_ID`, and (optionally) `BURGEE_JWT_SCOPE`. Users are auto-provisioned on first login. Set `BURGEE_ADMIN_SUBJECT` to the token `sub` of the initial admin.

> The provider must issue **JWT access tokens** for the configured audience so the resource server can validate them against the issuer (e.g. Keycloak does this by default; Auth0 requires an API/audience).

### Roles

| Role    | Permissions                                                    |
| ------- | -------------------------------------------------------------- |
| `ADMIN` | Full access — manage flags, users, and all admin endpoints     |
| `USER`  | Authenticated access — view flags and audit log                |
| `NEW`   | Default for auto-provisioned SSO users — no permissions until upgraded by an admin |

## Configuration

| Variable                | Default                                   | Description                                     |
| ----------------------- |-------------------------------------------| ----------------------------------------------- |
| `BURGEE_STORAGE`        | `postgres`                                | Storage mode: `postgres`, or `memory` for evaluation/dev (data is lost on restart) |
| `DB_URL`                | `jdbc:postgresql://localhost:5432/burgee` | JDBC URL (postgres storage mode)                |
| `DB_USERNAME`           | `burgee`                                  | Database user                                   |
| `DB_PASSWORD`           | `burgee`                                  | Database password                               |
| `SERVER_PORT`           | `8080`                                    | Backend HTTP port                               |
| `BURGEE_AUTH_METHOD`    | `basic`                                   | Auth method: `basic` or `jwt`                   |
| `BURGEE_ADMIN_USERNAME` | `admin`                                   | Bootstrap admin username (basic auth)            |
| `BURGEE_ADMIN_PASSWORD` | `admin`                                   | Bootstrap admin password (basic auth)            |
| `BURGEE_ADMIN_SUBJECT`  | *(empty)*                                 | IDP subject to bootstrap as admin (jwt)         |
| `BURGEE_JWT_ISSUER_URI` | *(empty)*                                 | OIDC issuer URI (jwt auth)                       |
| `BURGEE_JWT_CLIENT_ID`  | *(empty)*                                 | Public SPA client id (jwt auth)                  |
| `BURGEE_JWT_SCOPE`      | `openid profile email`                    | Scopes requested by the SPA (jwt auth)           |
| `BURGEE_PORT`           | `8080`                                    | Host port published by `docker compose`          |

## Statelessness

In both `basic` and `jwt` modes the backend keeps no session state — authentication is validated per request. Every replica reads/writes the same Postgres, so you can run as many backend containers as you like behind a load balancer. (This does not hold for the in-memory storage mode, where each process keeps its own data.)

## Backend architecture

The backend follows hexagonal architecture (ports & adapters). Each bounded context (`flag`, `user`) has the same structure:

```
io.github.janverhoeckx.burgee.{flag,user}/
├── domain/                                # pure domain model + invariants
├── application/
│   ├── port/inbound/                      # use case interfaces (driving ports)
│   ├── port/outbound/                     # repository / SPI interfaces (driven ports)
│   └── service/                           # use case implementations
└── adapter/
    ├── inbound/web/                       # Spring MVC controllers, DTOs, exception handler
    ├── inbound/bootstrap/                 # startup runners (e.g. bootstrap admin)
    └── outbound/persistence/              # Spring Data JDBC row, repository, port adapter
```

Controllers depend only on use case interfaces; the service depends only on the outbound port. The Spring Data JDBC row class and `CrudRepository` live behind the persistence adapter and are invisible to the rest of the application. Domain models are fully immutable; updates produce a new instance via `copy`, mirroring how Spring Data JDBC treats aggregates.

## Local development

```bash
./dev.sh             # in-memory storage mode, no Docker needed
./dev.sh --postgres  # Postgres in Docker, data survives restarts
```

`./dev.sh` starts the backend via `./mvnw spring-boot:run` on http://localhost:8080 and the Angular dev server on http://localhost:4200 (override with `BURGEE_FRONTEND_PORT`). If either process exits, the other is stopped too. Settings from `.env` are picked up, same as with `docker compose`. Ctrl-C stops the backend and frontend.

By default the backend runs in the in-memory storage mode, so no Docker or Postgres is needed, and all data is lost when it stops. With `--postgres` it starts Postgres in Docker first (published on `localhost:5432`, override with `BURGEE_DB_PORT`). Use that when you work on migrations or persistence. Postgres keeps running after Ctrl-C (`docker compose stop postgres` to stop it).

The dev server proxies `/api` to the backend (see `frontend/proxy.conf.json`), so open the dashboard on the dev server port.

To run the parts separately:

```bash
(cd backend && BURGEE_STORAGE=memory ./mvnw spring-boot:run)   # or start Postgres first, see below
(cd frontend && npm install && npm start)

docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --wait postgres
(cd backend && ./mvnw spring-boot:run)
```

In production the SPA is served by the backend at `/`, so no proxy is needed.

### End-to-end tests

The Playwright suite in `e2e/` runs against the production Docker image on a throwaway Postgres (see ADR 0003). It needs Docker and Node 22+.

```bash
cd e2e
npm ci && npx playwright install chromium   # once
npm run e2e                                  # build the image, start the stack, run all tests, tear down
npx playwright test tests/login.spec.ts      # one spec file
npm run report                               # open the HTML report of the last run
```

The stack runs as compose project `burgee-e2e` with the app on http://localhost:18080 and a fresh in-memory Postgres every run, so it never touches `dev.sh` or your `docker compose up` data.

| Variable         | Default      | Description                                                    |
|------------------|--------------|----------------------------------------------------------------|
| `E2E_NO_BUILD`   | *(unset)*    | `1` skips the image build and reuses `E2E_IMAGE`              |
| `E2E_KEEP_STACK` | *(unset)*    | `1` leaves the stack running after the run, for debugging     |
| `E2E_IMAGE`      | `burgee:e2e` | Image to run                                                   |
| `E2E_PORT`       | `18080`      | Host port the app is published on                              |
| `E2E_PROJECT`    | `burgee-e2e` | Compose project name, for running several stacks side by side  |

## Roadmap

- Environments (dev/staging/prod) per flag
- Percentage rollouts and more Condition operators
- API tokens for service auth

## License

MIT — see [LICENSE](LICENSE).
