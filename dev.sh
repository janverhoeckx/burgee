#!/usr/bin/env bash
# Starts a local dev environment: backend (spring-boot:run) and frontend (ng serve).
#   ./dev.sh             in-memory storage mode: no Docker or Postgres needed, data is lost on restart
#   ./dev.sh --postgres  Postgres (docker) storage mode; Postgres keeps running after Ctrl-C
#                        (stop with `docker compose stop postgres`)
# Ctrl-C stops the backend and frontend.
set -euo pipefail

cd "$(dirname "$0")"

storage=memory
case "${1:-}" in
  "") ;;
  --postgres) storage=postgres ;;
  *)
    echo "Usage: $0 [--postgres]" >&2
    exit 2
    ;;
esac

# Load .env like docker compose does, so the backend sees the same settings.
if [[ -f .env ]]; then
  while IFS= read -r line; do
    export "$line"
  done < <(grep -E '^[A-Za-z_][A-Za-z0-9_]*=' .env)
fi

export BURGEE_STORAGE="$storage"

trap 'trap - EXIT; kill 0' INT TERM EXIT

if [[ "$storage" == postgres ]]; then
  export DB_URL="jdbc:postgresql://localhost:${BURGEE_DB_PORT:-5432}/burgee"
  export DB_USERNAME=burgee
  export DB_PASSWORD="${DB_PASSWORD:-burgee}"

  echo "Starting Postgres..."
  docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --wait postgres
else
  echo "Using the in-memory storage mode: all data is lost when the backend stops (use --postgres to keep it)."
fi

if [[ ! -d frontend/node_modules ]]; then
  echo "Installing frontend dependencies..."
  (cd frontend && npm install)
fi

FRONTEND_PORT="${BURGEE_FRONTEND_PORT:-4200}"
echo "Starting backend on http://localhost:8080 and frontend on http://localhost:${FRONTEND_PORT}..."
(cd backend && ./mvnw spring-boot:run) &
backend_pid=$!
(cd frontend && npm start -- --port "$FRONTEND_PORT") &
frontend_pid=$!

# Stop everything as soon as either process exits (macOS bash 3.2 has no `wait -n`).
while kill -0 "$backend_pid" 2>/dev/null && kill -0 "$frontend_pid" 2>/dev/null; do
  sleep 1
done
echo "A dev process exited; shutting down." >&2
