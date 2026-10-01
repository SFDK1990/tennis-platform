#!/usr/bin/env bash
# Deploys a tag (or branch) on the server: checkout, rebuild, restart (Fase 17, 9).
#   TennisPlatformApp/deploy/deploy.sh v1.17.1
set -euo pipefail

REF="${1:?usage: deploy.sh <tag-or-branch>}"
APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$APP_DIR"

if [[ ! -f .env ]]; then
  echo "Missing $APP_DIR/.env (see deploy/.env.prod.example)" >&2
  exit 1
fi

git fetch --tags --prune origin
git checkout --detach "$REF" 2>/dev/null || git checkout --detach "origin/$REF"

docker compose -f compose.prod.yaml up -d --build --remove-orphans
docker image prune -f >/dev/null
docker compose -f compose.prod.yaml ps
