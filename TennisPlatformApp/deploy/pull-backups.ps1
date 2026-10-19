# Copies the server's encrypted backups to this PC over Tailscale (Fase 17, 5).
# Scheduled daily on the PC (33-operacion.md); a run missed while the PC is off happens at the
# next start. The files stay encrypted here: the private key is not on this PC either.
# Fails when the newest backup is older than two days, so a stopped cron on the server shows
# up as a failed task here instead of going unnoticed.
param(
    [string]$Server = 'daniel@tenis-pi',
    [string]$Destination = (Join-Path $env:USERPROFILE 'Backups\tennis-platform'),
    [int]$RetentionDays = 14
)
$ErrorActionPreference = 'Stop'

New-Item -ItemType Directory -Force $Destination | Out-Null

# -p keeps the server's modification time, which the retention below relies on.
& scp -B -p "${Server}:/srv/backups/tennis-*.sql.gz.age" $Destination
if ($LASTEXITCODE -ne 0) { throw "scp from $Server failed with exit code $LASTEXITCODE" }

$backups = Get-ChildItem $Destination -Filter 'tennis-*.sql.gz.age'
$backups | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
    Remove-Item

$newest = $backups | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $newest -or $newest.LastWriteTime -lt (Get-Date).AddDays(-2)) {
    throw "No backup from the last two days in $Destination"
}
Write-Output "Newest backup: $($newest.Name)"
