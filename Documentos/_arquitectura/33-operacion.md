# Operación

Lo que se consulta con prisa. Todo se ejecuta en el servidor, en `/srv/tennis-platform/TennisPlatformApp`,
salvo lo que dice «en el PC». El porqué de cada decisión está en `29-fase17-analisis-endurecimiento-despliegue.md`.

## Desplegar

```bash
deploy/deploy.sh v1.17.1
```

Hace checkout del tag, reconstruye y reinicia. Para volver atrás, el mismo comando con el tag
anterior. Si el tag trae una migración, antes hay que tener el backup de esa noche.

## Backups

Cada noche `deploy/backup.sh` deja un volcado cifrado en `/srv/backups` y borra los de más de
14 días. El PC los trae por Tailscale con `deploy/pull-backups.ps1`, que falla si el último
tiene más de dos días. La clave privada no está ni en el servidor ni en el PC: está en tu gestor
de contraseñas.

Activarlo en el servidor, una vez:

```bash
sudo apt install age
```

```bash
crontab -e   # 30 3 * * * /srv/tennis-platform/TennisPlatformApp/deploy/backup.sh >> /srv/backups/backup.log 2>&1
```

`deploy/backup.pub` es la clave pública (`age-keygen -y clave.txt`). La privada se genera una
vez en el PC con `age-keygen -o clave.txt`, se guarda en el gestor de contraseñas y el fichero
se borra.

En el PC, una vez, en PowerShell. El script se copia fuera del repositorio, porque una rama que
no lo tenga dejaría la tarea sin él:

```powershell
$script = "$env:USERPROFILE\Backups\pull-backups.ps1"
Copy-Item TennisPlatformApp\deploy\pull-backups.ps1 $script
$action = New-ScheduledTaskAction -Execute powershell.exe -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$script`""
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Minutes 10)
Register-ScheduledTask -TaskName 'Tennis Platform backups' -Action $action -Settings $settings -Trigger (New-ScheduledTaskTrigger -Daily -At 10am)
```

`StartWhenAvailable` hace que, si el PC estaba apagado a esa hora, la copia se haga al encenderlo.
Si la tarea aparece como fallida en el Programador de tareas, el servidor no está haciendo backups
o Tailscale SSH está esperando a que lo apruebes (el límite de 10 minutos corta esa espera).

## Restaurar

Sustituye la base entera por la del fichero. Primero lo descifra y lo comprueba; si el fichero
está corrupto o la clave no es la suya, para sin tocar la base.

1. Copiar el backup y la clave privada al servidor (`scp`).
2. Restaurar:

   ```bash
   AGE_KEY=~/clave.txt deploy/restore.sh ~/tennis-2026-10-17.sql.gz.age
   ```

3. Borrar la clave del servidor: `rm ~/clave.txt`.

El backend se para durante la carga y vuelve a arrancar. Lo escrito después del backup se pierde.

## Cambiar de servidor

El volcado es SQL plano, sin dueños: vale en otra arquitectura y en un Postgres igual o más nuevo.

1. Clonar el repositorio en `/srv/tennis-platform` y copiar el `.env` del servidor viejo
   (permisos `600`).
2. Arrancar sólo la base: `docker compose -f compose.prod.yaml up -d postgres`.
3. Restaurar el último backup, como arriba.
4. `deploy/deploy.sh <tag>` y activar el cron del backup.

Con el mismo `JWT_SECRET`, las sesiones abiertas siguen valiendo.

## Rotar un secreto

Se cambia en `.env` y se aplica con `docker compose -f compose.prod.yaml up -d`.

- `JWT_SECRET`: todos vuelven a iniciar sesión.
- `DB_PASSWORD`: Postgres guarda la contraseña en su volumen, así que antes se cambia en la base:
  `docker compose -f compose.prod.yaml exec postgres psql -U tennis_platform -c "alter user tennis_platform password '...'"`.
- `MAIL_PASSWORD`: se genera la nueva en el proveedor de correo y después se revoca la vieja.

## Bots y ataques

Caddy deja una línea por petición, sin IP, cookies ni el token de los enlaces de los correos.
`deploy/access-report.sh` la resume junto con los intentos contra la API: rutas que no existen
(ahí salen los escaneos de `wp-login.php` o `.env`), lo rechazado (401, 403, 429), las
contraseñas probadas y los bots que se identifican. Un 404 de un escaneo no es un problema; sí
lo son muchos `401 AUTH_BAD_CREDENTIALS` seguidos o `429`.

```bash
TennisPlatformApp/deploy/access-report.sh 24h
```

Cada lunes, en el servidor, una vez (el PC lo trae a `Backups\tennis-platform\informes` con los
backups, tras volver a copiar `pull-backups.ps1`):

```bash
mkdir -p /srv/reports
```

```bash
crontab -e   # 0 8 * * 1 /srv/tennis-platform/TennisPlatformApp/deploy/access-report.sh > /srv/reports/accesos-$(date +\%G-W\%V).txt 2>&1
```

## Estado del servidor

Temperatura, memoria, disco y carga de la Pi en una página que se abre desde el móvil:
https://tenis-pi.tail26506d.ts.net:8443. Sólo la ve un dispositivo con Tailscale y la sesión
iniciada en tu cuenta: la sirve `tailscaled` en un puerto que Funnel no abre, y Caddy no la
conoce. Se recarga sola cada minuto; si la hora de arriba se queda vieja y sale en rojo, el cron
dejó de ejecutarse.

Arriba sale un aviso si la Pi ha tenido falta de corriente o se ha ralentizado por calor desde que
arrancó (`vcgencmd get_throttled`). Desde 80 °C la Pi baja la frecuencia: la tarjeta se pone
naranja a 70 °C y roja a 80 °C; la memoria y el disco, a 80 % y 90 %.

Activarlo en el servidor, una vez. El script se copia fuera del repositorio, porque `deploy.sh`
cambia de etiqueta y una que no lo tenga dejaría el cron sin él:

```bash
mkdir -p ~/.local/bin /srv/status
git show origin/main:TennisPlatformApp/deploy/status-page.sh > ~/.local/bin/tennis-status-page.sh
chmod 755 ~/.local/bin/tennis-status-page.sh
~/.local/bin/tennis-status-page.sh   # la primera página, y comprueba que funciona
```

```bash
crontab -e   # * * * * * /home/daniel/.local/bin/tennis-status-page.sh
```

Servir un fichero exige root, así que este paso pide la contraseña. Desde el PC, con
`ssh -t daniel@tenis-pi "..."`:

```bash
sudo tailscale serve --bg --https=8443 /srv/status/index.html
tailscale funnel status   # 443 sale como Funnel; 8443 sólo como tailnet
```

Para quitarla: `sudo tailscale serve --https=8443 off` y la línea del cron.

## Logs

```bash
docker compose -f compose.prod.yaml logs backend --since 1h
```

Un error que ve un usuario lleva un código de referencia: son los 8 primeros caracteres del
correlation id, y basta con buscarlo (`| grep <código>`).
