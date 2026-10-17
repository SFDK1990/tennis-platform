#!/usr/bin/env bash
# Backs up the running development stack and restores it (29-fase17, tests of 17.1). Run in
# CI after the end-to-end flows, so the database holds the real schema and real rows:
#   TennisPlatformApp/deploy/test-backup-restore.sh
# A corrupt backup must fail and leave the data as it was; the good one must bring back
# every row, and the backend must start again on top of it.
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
export COMPOSE_FILE=compose.yaml
cd "$APP_DIR"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
age-keygen -o "$work/key" 2> /dev/null
age-keygen -y "$work/key" > "$work/key.pub"

# One line per table of the application: its name and its row count.
row_counts() {
  docker compose exec -T postgres psql -U tennis_platform -d tennis_platform -At -c "
    select table_name || ' ' || (xpath('/row/c/text()',
      query_to_xml(format('select count(*) as c from %I', table_name), false, true, '')))[1]
    from information_schema.tables
    where table_schema = 'public' and table_type = 'BASE TABLE'
    order by table_name"
}

before="$(row_counts)"
echo "$before"
grep -q '^users [1-9]' <<< "$before" || { echo 'The stack holds no users to restore' >&2; exit 1; }

BACKUP_DIR="$work" RECIPIENT_FILE="$work/key.pub" deploy/backup.sh
backup="$(ls "$work"/tennis-*.sql.gz.age)"

# A byte inverted in the middle: age authenticates what it decrypts, so this must not load.
cp "$backup" "$work/corrupt.age"
middle=$(($(stat -c %s "$work/corrupt.age") / 2))
byte="$(dd if="$work/corrupt.age" bs=1 skip="$middle" count=1 status=none | od -An -tu1 | tr -d ' ')"
printf "\\$(printf '%03o' $((255 - byte)))" \
  | dd of="$work/corrupt.age" bs=1 seek="$middle" conv=notrunc status=none
if AGE_KEY="$work/key" deploy/restore.sh "$work/corrupt.age"; then
  echo 'A corrupt backup was restored' >&2
  exit 1
fi
[[ "$(row_counts)" == "$before" ]] || { echo 'A failed restore changed the data' >&2; exit 1; }

# Rows written after the backup must disappear: the restore replaces, it does not merge.
docker compose exec -T postgres psql -U tennis_platform -d tennis_platform -q -c \
  "create table written_after_backup (id int)"

AGE_KEY="$work/key" deploy/restore.sh "$backup"
after="$(row_counts)"
if [[ "$after" != "$before" ]]; then
  diff <(echo "$before") <(echo "$after") >&2 || true
  echo 'The restored database differs from the one backed up' >&2
  exit 1
fi

for _ in $(seq 1 60); do
  curl --fail --silent http://localhost:8080/actuator/health > /dev/null && {
    echo 'Backup restored and the backend is healthy on it'
    exit 0
  }
  sleep 5
done
echo 'The backend did not become healthy on the restored database' >&2
exit 1
