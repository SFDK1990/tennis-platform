#!/usr/bin/env bash
# Makes sure the backend's own role exists, without superuser, and owns the database
# (deploy/postgres-init/app-role.psql). deploy.sh and restore.sh call it; idempotent.
#   TennisPlatformApp/deploy/app-role.sh
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yaml}"
cd "$APP_DIR"
if [[ -f .env ]]; then set -a; source .env; set +a; fi

docker compose -f "$COMPOSE_FILE" exec -T postgres psql -v ON_ERROR_STOP=1 --quiet \
  -U "${DB_USER:-tennis_platform}" -d "${DB_NAME:-tennis_platform}" \
  -v app_user="${DB_APP_USER:-tennis_app}" -v app_password="${DB_APP_PASSWORD:?DB_APP_PASSWORD is not set in .env}" \
  -f /docker-entrypoint-initdb.d/app-role.psql
