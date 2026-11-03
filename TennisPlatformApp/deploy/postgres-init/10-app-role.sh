#!/usr/bin/env bash
# Run by the postgres image the first time its volume is created (docker-entrypoint-initdb.d).
# A volume that already exists gets the role from deploy.sh instead (app-role.sh).
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v app_user="$DB_APP_USER" -v app_password="$DB_APP_PASSWORD" \
  -f /docker-entrypoint-initdb.d/app-role.psql
