#!/usr/bin/env bash
# Replaces the database with a backup written by backup.sh (Fase 17, 5). On the server:
#   AGE_KEY=~/backup.key TennisPlatformApp/deploy/restore.sh tennis-2026-10-17.sql.gz.age
# AGE_KEY is the private key, brought to the server only for the restore and deleted after.
# The file is decrypted and checked before the database is touched: a corrupt file, or the
# wrong key, stops here with the current data intact. On a new server, start only the
# database first (docker compose -f compose.prod.yaml up -d postgres), restore, then deploy.
set -euo pipefail

FILE="$(realpath "${1:?usage: restore.sh <backup.sql.gz.age>}")"
AGE_KEY="$(realpath "${AGE_KEY:?AGE_KEY must point to the private key file}")"
APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# The test in CI restores into the development stack.
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yaml}"

cd "$APP_DIR"
if [[ -f .env ]]; then set -a; source .env; set +a; fi
DB_USER="${DB_USER:-tennis_platform}"
DB_NAME="${DB_NAME:-tennis_platform}"
compose() { docker compose -f "$COMPOSE_FILE" "$@"; }

# The plain dump holds personal data: readable only by this user, and gone on exit.
umask 077
sql="$(mktemp)"
trap 'rm -f "$sql"' EXIT
age -d -i "$AGE_KEY" "$FILE" | gunzip > "$sql"
if ! tail -n 5 "$sql" | grep -q 'PostgreSQL database dump complete'; then
  echo "Not a complete pg_dump: $FILE" >&2
  exit 1
fi

# Stopped, the backend cannot write between the drop and the load. Restarted only if it was
# running: on a new server deploy.sh starts it.
backend_running="$(compose ps -q --status running backend)"
[[ -n "$backend_running" ]] && compose stop backend

compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d postgres \
  -c "DROP DATABASE IF EXISTS \"$DB_NAME\" WITH (FORCE)" \
  -c "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\""
compose exec -T postgres psql -v ON_ERROR_STOP=1 --single-transaction --quiet \
  -U "$DB_USER" -d "$DB_NAME" < "$sql" > /dev/null

[[ -n "$backend_running" ]] && compose start backend
echo "Restored $FILE into $DB_NAME"
