#!/usr/bin/env bash
# A page with the server's temperature, memory and disk, readable from the phone through the
# tailnet and never from the internet (33-operacion.md, "Estado del servidor").
# Every minute from cron, as the user that runs the backups (it needs the docker group):
#   * * * * * /home/daniel/.local/bin/tennis-status-page.sh
# Served by tailscaled itself, on a port Funnel does not open (once, as root):
#   tailscale serve --bg --https=8443 /srv/status/index.html
# Copied out of the repository: deploy.sh checks out tags, and one without this file would
# leave cron without it.
set -euo pipefail

STATUS_DIR="${STATUS_DIR:-/srv/status}"
mkdir -p "$STATUS_DIR"

# Optional readings: without vcgencmd (not a Pi) or docker, the page leaves them out.
throttled="$(vcgencmd get_throttled 2>/dev/null | cut -d= -f2 || true)"
containers="$(docker stats --no-stream --format '{{.Name}}\t{{.MemUsage}}\t{{.CPUPerc}}' 2>/dev/null || true)"

python3 - "$STATUS_DIR" "$throttled" "$containers" <<'PY'
import html, os, shutil, sys, time

status_dir, throttled, containers = sys.argv[1:4]
now = int(time.time())
HISTORY_MINUTES = 24 * 60

def read(path):
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return None

raw_temp = read("/sys/class/thermal/thermal_zone0/temp")
temp = int(raw_temp) / 1000 if raw_temp else None

meminfo = {}
for line in (read("/proc/meminfo") or "").splitlines():
    key, _, value = line.partition(":")
    meminfo[key] = int(value.split()[0]) * 1024
mem_total = meminfo.get("MemTotal", 0)
mem_used = mem_total - meminfo.get("MemAvailable", 0)
swap_used = meminfo.get("SwapTotal", 0) - meminfo.get("SwapFree", 0)
mem_pct = 100 * mem_used / mem_total if mem_total else 0

disk = shutil.disk_usage("/")
load = os.getloadavg()
uptime_days = float((read("/proc/uptime") or "0").split()[0]) / 86400

# One line per run, the last 24 hours, for the peaks and the curve.
history_path = os.path.join(status_dir, "history.csv")
rows = []
for line in (read(history_path) or "").splitlines():
    t, tc, mp = line.split(",")
    if now - int(t) < HISTORY_MINUTES * 60:
        rows.append((int(t), float(tc) if tc else None, float(mp)))
rows.append((now, temp, mem_pct))
with open(history_path + ".tmp", "w", encoding="utf-8") as f:
    f.writelines(f"{t},{'' if tc is None else f'{tc:.1f}'},{mp:.1f}\n" for t, tc, mp in rows)
os.replace(history_path + ".tmp", history_path)

def gib(n):
    return f"{n / 2**30:.1f} GB"

# The Pi lowers its clock from 80 °C; 70 is the warning that it is getting close.
def temp_level(t):
    return "ok" if t is None or t < 70 else "warn" if t < 80 else "bad"

def pct_level(p):
    return "ok" if p < 80 else "warn" if p < 90 else "bad"

temps = [tc for _, tc, _ in rows if tc is not None]
peak_mem = max(mp for _, _, mp in rows)

def sparkline(values, low, high):
    if len(values) < 2:
        return ""
    w, h = 300, 60
    span = (high - low) or 1
    step = w / (len(values) - 1)
    points = " ".join(f"{i * step:.1f},{h - (min(max(v, low), high) - low) / span * h:.1f}"
                      for i, v in enumerate(values))
    return (f'<svg viewBox="0 0 {w} {h}" preserveAspectRatio="none" aria-hidden="true">'
            f'<polyline points="{points}"/></svg>')

# vcgencmd get_throttled: the low bits are now, the same bits << 16 are "since it booted".
FLAGS = {0: "falta de corriente", 1: "frecuencia limitada", 2: "ralentizada",
         3: "límite de temperatura"}
alerts = []
if throttled:
    bits = int(throttled, 16)
    now_flags = [name for bit, name in FLAGS.items() if bits & (1 << bit)]
    past_flags = [name for bit, name in FLAGS.items() if bits & (1 << (bit + 16))]
    if now_flags:
        alerts.append(("bad", "Ahora: " + ", ".join(now_flags)))
    if past_flags:
        alerts.append(("warn", "Desde que arrancó: " + ", ".join(past_flags)))

container_rows = ""
for line in containers.splitlines():
    name, usage, cpu = line.split("\t")
    name = name.removeprefix("tennis-prod-").removesuffix("-1")
    used, _, limit = usage.partition(" / ")
    container_rows += (f"<tr><td>{html.escape(name)}</td><td>{html.escape(used)}</td>"
                       f"<td>{html.escape(limit)}</td><td>{html.escape(cpu)}</td></tr>")

def card(title, value, level, detail, chart=""):
    return (f'<section class="{level}"><h2>{title}</h2><p class="value">{value}</p>'
            f'{chart}<p class="detail">{detail}</p></section>')

temp_text = f"{temp:.1f}\u00a0°C" if temp is not None else "—"
temp_detail = (f"24 h: mín. {min(temps):.1f}\u00a0°C, máx. {max(temps):.1f}\u00a0°C" if temps else "")
cards = [
    card("Temperatura", temp_text, temp_level(temp), temp_detail,
         sparkline(temps, 30, 85)),
    card("Memoria", f"{mem_pct:.0f}\u00a0%", pct_level(mem_pct),
         f"{gib(mem_used)} de {gib(mem_total)} · swap {gib(swap_used)}"
         f" · máx. 24 h {peak_mem:.0f}\u00a0%",
         sparkline([mp for _, _, mp in rows], 0, 100)),
    card("Disco", f"{100 * disk.used / disk.total:.0f}\u00a0%", pct_level(100 * disk.used / disk.total),
         f"{gib(disk.used)} de {gib(disk.total)} · libres {gib(disk.free)}"),
    card("Carga", f"{load[0]:.2f}", "ok",
         f"5 min {load[1]:.2f} · 15 min {load[2]:.2f} · {os.cpu_count()} núcleos"
         f" · encendida hace {uptime_days:.1f} días"),
]

alert_html = "".join(f'<p class="alert {level}">{html.escape(text)}</p>' for level, text in alerts)
table = (f"<h2>Contenedores</h2><table><tr><th></th><th>Memoria</th><th>Límite</th><th>CPU</th></tr>"
         f"{container_rows}</table>") if container_rows else ""
stamp = time.strftime("%d/%m/%Y %H:%M", time.localtime(now))

page = f"""<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="refresh" content="60">
<title>Estado de la Pi</title>
<style>
:root {{ color-scheme: light dark; --bg:#f6f7f5; --card:#fff; --fg:#1d2420; --muted:#66706a;
  --ok:#2f8f4e; --warn:#c98a0b; --bad:#c2402f; }}
@media (prefers-color-scheme: dark) {{ :root {{ --bg:#141816; --card:#1e2421; --fg:#e8ece9;
  --muted:#9aa49e; }} }}
body {{ margin:0; padding:16px; background:var(--bg); color:var(--fg);
  font:16px/1.4 system-ui, sans-serif; max-width:720px; margin-inline:auto; }}
h1 {{ font-size:1.3rem; margin:0 0 4px; }}
.stamp {{ color:var(--muted); margin:0 0 16px; }}
.stamp.stale {{ color:var(--bad); font-weight:600; }}
.grid {{ display:grid; gap:12px; grid-template-columns:repeat(auto-fit, minmax(200px, 1fr)); }}
section {{ background:var(--card); border-radius:12px; padding:14px; border-left:6px solid var(--ok); }}
section.warn {{ border-color:var(--warn); }} section.bad {{ border-color:var(--bad); }}
h2 {{ font-size:.85rem; text-transform:uppercase; letter-spacing:.05em; color:var(--muted); margin:0; }}
.value {{ font-size:2rem; font-weight:700; margin:4px 0; }}
.detail {{ color:var(--muted); font-size:.9rem; margin:4px 0 0; }}
svg {{ width:100%; height:48px; display:block; }}
polyline {{ fill:none; stroke:currentColor; stroke-width:2; vector-effect:non-scaling-stroke; opacity:.6; }}
.alert {{ padding:10px 14px; border-radius:10px; margin:0 0 12px; color:#fff; background:var(--bad); }}
.alert.warn {{ background:var(--warn); }}
table {{ width:100%; border-collapse:collapse; background:var(--card); border-radius:12px; overflow:hidden; }}
td, th {{ padding:8px 12px; text-align:left; font-variant-numeric:tabular-nums; }}
tr + tr td {{ border-top:1px solid color-mix(in srgb, var(--muted) 25%, transparent); }}
body > h2 {{ margin:20px 0 8px; }}
</style></head><body>
<h1>Estado de la Pi</h1>
<p class="stamp" data-at="{now}">Actualizado el {stamp}</p>
{alert_html}
<div class="grid">{"".join(cards)}</div>
{table}
<script>
// cron rewrites the page every minute: an old timestamp means the script stopped.
const s = document.querySelector('.stamp');
if (Date.now() / 1000 - Number(s.dataset.at) > 180) {{
  s.textContent += ' · sin datos nuevos desde entonces';
  s.classList.add('stale');
}}
</script>
</body></html>
"""
index = os.path.join(status_dir, "index.html")
with open(index + ".tmp", "w", encoding="utf-8") as f:
    f.write(page)
os.replace(index + ".tmp", index)
PY
