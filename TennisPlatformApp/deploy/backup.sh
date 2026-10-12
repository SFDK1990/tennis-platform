#!/usr/bin/env bash
# Daily encrypted dump of the database (Fase 17, 5). Run from cron on the server:
#   30 3 * * * /srv/tennis-platform/TennisPlatformApp/deploy/backup.sh
# Needs: age (apt install age) and the PUBLIC key in deploy/backup.pub (age-keygen -y).
# The private key never stays on the server: without it a stolen dump is unreadable.
# Copying the file off the server (rclone) is the next line of the same cron entry.
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/srv/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
RECIPIENT_FILE="$APP_DIR/deploy/backup.pub"

cd "$APP_DIR"
set -a; source .env; set +a
mkdir -p "$BACKUP_DIR"

out="$BACKUP_DIR/tennis-$(date +%F).sql.gz.age"
docker compose -f compose.prod.yaml exec -T postgres \
  pg_dump --clean --if-exists -U "$DB_USER" "$DB_NAME" \
  | gzip | age -R "$RECIPIENT_FILE" > "$out.tmp"
mv "$out.tmp" "$out"

find "$BACKUP_DIR" -name 'tennis-*.sql.gz.age' -mtime +"$RETENTION_DAYS" -delete
echo "Backup written: $out"
