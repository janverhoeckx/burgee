# Burgee

A simple, self-hostable, open-source feature flag service.

- **Stateless backend** — Spring Boot 4 + Kotlin + Spring Data JDBC + Postgres, scales horizontally behind any load balancer.
- **Hexagonal architecture** — domain, ports, and adapters cleanly separated; swapping persistence or transport requires no changes to the core.
- **Admin dashboard** — Angular 21 SPA, built into the backend image and served from the same origin.
- **User management** — create, edit, and delete users from the dashboard. Role-based access control with three roles: Admin, User, and New.
- **Flexible authentication** — HTTP Basic (default), OAuth2/OIDC, or Firebase Auth. Switch with a single environment variable.
- **Auto-provisioning** — SSO users are automatically created on first login with the `NEW` role; an admin upgrades them.
- **Public REST API** — fetch flags from your apps with a single GET.
- **Single container** — frontend and backend ship together; one image, one port.

## Quickstart

```bash
docker compose up --build
```

Then open:

- **Dashboard**: http://localhost:8080 (default login: `admin` / `admin`)
- **Public flags API**: http://localhost:8080/api/v1/flags

> **Change the default admin credentials** before exposing Burgee anywhere. Set `BURGEE_ADMIN_USERNAME` and `BURGEE_ADMIN_PASSWORD` in your environment or a `.env` file. For OAuth2 or Firebase, set `BURGEE_AUTH_METHOD` and `BURGEE_ADMIN_SUBJECT` instead.

## REST API

### Public (no auth)

```
GET  /api/v1/flags           → [{ "key": "checkout-v2", "enabled": true }, …]
GET  /api/v1/flags/{key}     → { "key": "checkout-v2", "enabled": true }
```

### Admin — Flags (requires `ADMIN` role)

```
GET    /api/admin/flags
GET    /api/admin/flags/{id}
POST   /api/admin/flags                 { "key": "...", "name": "...", "description": "...", "enabled": false }
PUT    /api/admin/flags/{id}            { "name": "...", "description": "...", "enabled": true }
POST   /api/admin/flags/{id}/toggle
DELETE /api/admin/flags/{id}
```

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
GET    /api/auth/info                   → { "method": "basic", "providers": [], "firebase": null }  (public)
GET    /api/auth/user                   → { "name": "admin", "role": "ADMIN", "isAdmin": true }     (authenticated)
```

Example:

```bash
curl -u admin:admin -X POST http://localhost:8080/api/admin/flags \
  -H 'Content-Type: application/json' \
  -d '{"key":"new-checkout","name":"New checkout","enabled":false}'
```

## Authentication

Burgee supports three authentication methods, selected via `BURGEE_AUTH_METHOD`:

### HTTP Basic (default)

Stateless, no external identity provider needed. Users are stored in the database with bcrypt-hashed passwords. A bootstrap admin is created on startup from `BURGEE_ADMIN_USERNAME` / `BURGEE_ADMIN_PASSWORD`.

### OAuth2 / OIDC

Session-based. Configure your OIDC provider via standard Spring Security properties (see `application.yml` comments). Users are auto-provisioned on first login with the `NEW` role. Set `BURGEE_ADMIN_SUBJECT` to the IDP subject of the user that should be bootstrapped as admin.

### Firebase Auth

Stateless JWT validation. The backend validates Firebase ID tokens; the frontend uses the Firebase JS SDK. Set `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY`, and `FIREBASE_AUTH_DOMAIN`. Users are auto-provisioned on first login. Set `BURGEE_ADMIN_SUBJECT` to the Firebase UID of the initial admin.

### Roles

| Role    | Permissions                                                    |
| ------- | -------------------------------------------------------------- |
| `ADMIN` | Full access — manage flags, users, and all admin endpoints     |
| `USER`  | Authenticated access — view flags and audit log                |
| `NEW`   | Default for auto-provisioned SSO users — no permissions until upgraded by an admin |

## Configuration

| Variable                | Default                                  | Description                                     |
| ----------------------- | ---------------------------------------- | ----------------------------------------------- |
| `DB_URL`                | `jdbc:postgresql://localhost:5432/burgee` | JDBC URL                                        |
| `DB_USERNAME`           | `burgee`                                 | Database user                                   |
| `DB_PASSWORD`           | `burgee`                                 | Database password                               |
| `SERVER_PORT`           | `8080`                                   | Backend HTTP port                               |
| `BURGEE_AUTH_METHOD`    | `basic`                                  | Auth method: `basic`, `oauth2`, or `firebase`   |
| `BURGEE_ADMIN_USERNAME` | `admin`                                  | Bootstrap admin username (basic auth)            |
| `BURGEE_ADMIN_PASSWORD` | `admin`                                  | Bootstrap admin password (basic auth)            |
| `BURGEE_ADMIN_SUBJECT`  | *(empty)*                                | IDP subject to bootstrap as admin (oauth2/firebase) |
| `FIREBASE_PROJECT_ID`   | *(empty)*                                | Firebase project ID (firebase auth)              |
| `FIREBASE_API_KEY`      | *(empty)*                                | Firebase web API key (firebase auth)             |
| `FIREBASE_AUTH_DOMAIN`  | *(empty)*                                | Firebase auth domain (firebase auth)             |
| `BURGEE_PORT`           | `8080`                                   | Host port published by `docker compose`          |

## Statelessness

In `basic` and `firebase` modes the backend keeps no session state — authentication is validated per request. In `oauth2` mode the backend uses server-side sessions. In all modes every replica reads/writes the same Postgres, so you can run as many backend containers as you like behind a load balancer.

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

### Backend

```bash
cd backend
docker run --rm -d --name burgee-pg \
  -e POSTGRES_DB=burgee -e POSTGRES_USER=burgee -e POSTGRES_PASSWORD=burgee \
  -p 5432:5432 postgres:16-alpine
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

The Angular dev server runs on http://localhost:4200. Point it at a local backend by setting `window.__burgeeConfig.apiBaseUrl` (e.g. via a small script tag during dev). In production the SPA is served by the backend at `/`, so no proxy is needed.

## Roadmap

- Environments (dev/staging/prod) per flag
- Targeting rules / percentage rollouts
- API tokens for service auth

## License

MIT — see [LICENSE](LICENSE).
