#!/usr/bin/env bash
# What bots and scanners tried against the server, from Caddy's access log and the backend's
# request lines. Neither carries an IP or an email, so neither does the report.
#   TennisPlatformApp/deploy/access-report.sh        # the last 7 days
#   TennisPlatformApp/deploy/access-report.sh 24h
# Weekly from cron, into /srv/reports (pulled by the PC with the backups, pull-backups.ps1):
#   0 8 * * 1 /srv/tennis-platform/TennisPlatformApp/deploy/access-report.sh > /srv/reports/accesos-$(date +\%G-W\%V).txt
set -euo pipefail

SINCE="${1:-168h}"
CADDY="${CADDY_CONTAINER:-tennis-prod-caddy-1}"
BACKEND="${BACKEND_CONTAINER:-tennis-prod-backend-1}"

caddy_log="$(mktemp)"
backend_log="$(mktemp)"
trap 'rm -f "$caddy_log" "$backend_log"' EXIT
docker logs --since "$SINCE" "$CADDY" >"$caddy_log" 2>&1 || true
docker logs --since "$SINCE" "$BACKEND" >"$backend_log" 2>&1 || true

python3 - "$SINCE" "$caddy_log" "$backend_log" <<'PY'
import collections, json, re, sys

sys.stdout.reconfigure(encoding="utf-8")

since, caddy_path, backend_path = sys.argv[1:4]
BOT = re.compile(r"bot|crawl|spider|scan|curl|wget|python|go-http|java/|zgrab|masscan|nmap|nikto|sqlmap|httpclient|libwww", re.I)

statuses, missing, agents, denied = collections.Counter(), collections.Counter(), collections.Counter(), collections.Counter()
total = 0
for line in open(caddy_path, encoding="utf-8", errors="replace"):
    try:
        entry = json.loads(line)
    except ValueError:
        continue
    if not entry.get("logger", "").startswith("http.log.access"):
        continue
    total += 1
    status = entry.get("status", 0)
    request = entry.get("request", {})
    path = request.get("uri", "").split("?")[0]
    agent = (request.get("headers", {}).get("User-Agent") or ["(sin navegador)"])[0]
    statuses[f"{status // 100}xx"] += 1
    if status == 404:
        missing[f"{request.get('method', '?')} {path}"] += 1
    if status in (401, 403, 429):
        denied[f"{status} {request.get('method', '?')} {path}"] += 1
    if BOT.search(agent) or agent == "(sin navegador)":
        agents[agent[:90]] += 1

# The backend's own line per API request: "POST /api/v1/auth/login 401 AUTH_BAD_CREDENTIALS 12 ms".
api = collections.Counter()
request_line = re.compile(r"(GET|POST|PUT|PATCH|DELETE) (/api/\S+) (401|403|429) (\S+)")
for line in open(backend_path, encoding="utf-8", errors="replace"):
    match = request_line.search(line)
    if match:
        method, path, status, code = match.groups()
        path = re.sub(r"[0-9a-f]{8}-[0-9a-f-]{27}", "{id}", path)
        api[f"{status} {code} {method} {path}"] += 1

def section(title, counter, limit=15):
    print(f"\n{title}")
    if not counter:
        print("  (nada)")
    for key, count in counter.most_common(limit):
        print(f"  {count:6}  {key}")

print(f"Accesos al servidor, últimas {since}")
print(f"Peticiones registradas: {total}")
section("Por código", statuses)
section("Rutas que no existen (aquí salen los escaneos: wp-login, .env, phpmyadmin...)", missing)
section("Rechazadas por Caddy o la app (401, 403, 429)", denied)
section("Intentos contra la API (contraseñas probadas, límite de intentos)", api)
section("Bots y herramientas que se identifican", agents)
PY
