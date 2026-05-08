# Burgee

A simple, self-hostable, open-source feature flag service.

- **Stateless backend** — Spring Boot + Kotlin + Postgres, scales horizontally behind any load balancer.
- **Admin dashboard** — Angular 19 SPA for creating, editing, and toggling flags.
- **Public REST API** — fetch flags from your apps with a single GET.
- **Docker-first** — `docker compose up` and you're running.

## Quickstart

```bash
docker compose up --build
```

Then open:

- **Dashboard**: http://localhost:8081 (default login: `admin` / `admin`)
- **Backend**: http://localhost:8080
- **Public flags API**: http://localhost:8080/api/v1/flags

> **Change the default admin password** before exposing Burgee anywhere. Set `BURGEE_ADMIN_USERNAME` and `BURGEE_ADMIN_PASSWORD` in your environment or a `.env` file.

## REST API

### Public (no auth)

```
GET  /api/v1/flags           → [{ "key": "checkout-v2", "enabled": true }, …]
GET  /api/v1/flags/{key}     → { "key": "checkout-v2", "enabled": true }
```

### Admin (HTTP Basic auth)

```
GET    /api/admin/flags
GET    /api/admin/flags/{id}
POST   /api/admin/flags                 { "key": "...", "name": "...", "description": "...", "enabled": false }
PUT    /api/admin/flags/{id}            { "name": "...", "description": "...", "enabled": true }
POST   /api/admin/flags/{id}/toggle
DELETE /api/admin/flags/{id}
```

Example:

```bash
curl -u admin:admin -X POST http://localhost:8080/api/admin/flags \
  -H 'Content-Type: application/json' \
  -d '{"key":"new-checkout","name":"New checkout","enabled":false}'
```

## Configuration

| Variable                  | Default                                    | Description                            |
| ------------------------- | ------------------------------------------ | -------------------------------------- |
| `DB_URL`                  | `jdbc:postgresql://localhost:5432/burgee`  | JDBC URL                               |
| `DB_USERNAME`             | `burgee`                                   | Database user                          |
| `DB_PASSWORD`             | `burgee`                                   | Database password                      |
| `SERVER_PORT`             | `8080`                                     | Backend HTTP port                      |
| `BURGEE_ADMIN_USERNAME`   | `admin`                                    | Dashboard / admin API user             |
| `BURGEE_ADMIN_PASSWORD`   | `admin`                                    | Dashboard / admin API password         |
| `BURGEE_API_BASE_URL`     | `http://backend:8080` (frontend container) | Backend URL the dashboard proxies to   |

## Statelessness

The backend keeps no session state. Authentication is HTTP Basic, validated per request against the configured admin credentials. Every replica reads/writes the same Postgres, so you can run as many backend containers as you like behind a round-robin load balancer.

## Local development

### Backend

```bash
cd backend
docker run --rm -d --name burgee-pg \
  -e POSTGRES_DB=burgee -e POSTGRES_USER=burgee -e POSTGRES_PASSWORD=burgee \
  -p 5432:5432 postgres:16-alpine
./gradlew bootRun
```

### Frontend

```bash
cd frontend
npm install
npm start
```

The Angular dev server runs on http://localhost:4200. Configure the API base URL by setting `window.__burgeeConfig.apiBaseUrl` (e.g. via a small script tag during dev), or use the nginx proxy via the Docker setup.

## Roadmap (not in v1)

- Environments (dev/staging/prod) per flag
- Targeting rules / percentage rollouts
- API tokens for service auth
- Audit log

## License

MIT — see [LICENSE](LICENSE).
