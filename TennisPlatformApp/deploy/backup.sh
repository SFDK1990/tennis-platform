#!/usr/bin/env bash
# Daily encrypted dump of the database (Fase 17, 5). Run from cron on the server:
#   30 3 * * * /srv/tennis-platform/TennisPlatformApp/deploy/backup.sh
# Needs: age (apt install age) and the PUBLIC key in deploy/backup.pub (age-keygen -y).
# The private key never stays on the server: without it a stolen dump is unreadable.
# The copy off the server is pulled by the PC (pull-backups.ps1): the server holds no
# credential to reach it, so whoever takes the server cannot delete those copies.
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/srv/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
RECIPIENT_FILE="${RECIPIENT_FILE:-$APP_DIR/deploy/backup.pub}"
# The test in CI dumps the development stack.
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yaml}"

cd "$APP_DIR"
if [[ -f .env ]]; then set -a; source .env; set +a; fi
DB_USER="${DB_USER:-tennis_platform}"
DB_NAME="${DB_NAME:-tennis_platform}"
mkdir -p "$BACKUP_DIR"

# Without owners or grants, so it restores under any database user, on another server too.
out="$BACKUP_DIR/tennis-$(date +%F).sql.gz.age"
docker compose -f "$COMPOSE_FILE" exec -T postgres \
  pg_dump --clean --if-exists --no-owner --no-privileges -U "$DB_USER" "$DB_NAME" \
  | gzip | age -R "$RECIPIENT_FILE" > "$out.tmp"
mv "$out.tmp" "$out"

find "$BACKUP_DIR" -name 'tennis-*.sql.gz.age' -mtime +"$RETENTION_DAYS" -delete
echo "Backup written: $out"
