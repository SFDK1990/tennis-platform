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

En el PC, una vez, en PowerShell:

```powershell
$script = "$env:USERPROFILE\Desktop\Casa\Proyectos Paginas Web\Tennis Platform\TennisPlatformApp\deploy\pull-backups.ps1"
$action = New-ScheduledTaskAction -Execute powershell.exe -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$script`""
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable
Register-ScheduledTask -TaskName 'Tennis Platform backups' -Action $action -Settings $settings -Trigger (New-ScheduledTaskTrigger -Daily -At 10am)
```

`StartWhenAvailable` hace que, si el PC estaba apagado a esa hora, la copia se haga al encenderlo.
Si la tarea aparece como fallida en el Programador de tareas, el servidor no está haciendo backups.

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

## Logs

```bash
docker compose -f compose.prod.yaml logs backend --since 1h
```

Un error que ve un usuario lleva un código de referencia: son los 8 primeros caracteres del
correlation id, y basta con buscarlo (`| grep <código>`).
