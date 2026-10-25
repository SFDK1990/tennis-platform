# Datos de ejemplo

Para ver la app con contenido en un entorno de desarrollo: seis alumnos con su nivel y su ficha,
tres grupos semanales (iniciación, intermedio y competición), clases sueltas de varios niveles y
abiertas, reservas, una clase llena con lista de espera y clases de la semana pasada con
asistencia. Las clases de ejemplo llevan en las notas «Ejemplo:».

Los datos son inventados: en un servidor con alumnos de verdad no se cargan, y antes de abrir la
app se quitan. La contraseña de los alumnos de ejemplo se elige al cargarlos y no se guarda en el
repositorio.

Con el stack levantado (`docker compose up -d --build`), desde la raíz del repositorio:

```bash
python TennisPlatformApp/demo/load-demo.py --env TennisPlatformApp/.env --student-password <contraseña>
```

- Como profesor, se entra con `TEACHER_EMAIL` y `TEACHER_PASSWORD` del `.env`.
- Como alumno, con `demo-lucia@demo.oasis.local` (iniciación), `demo-carlos@…` (sin nivel),
  `demo-pablo@…` y `demo-marta@…` (intermedio), `demo-ines@…` (avanzado) o `demo-diego@…`
  (competición), con la contraseña que se dio al cargarlos.

En la Pi, para enseñar la app antes de abrirla, con `--server`: así no sale ningún correo (los
alumnos y sus reservas se escriben en la base con la contraseña cifrada del entorno local, que
tiene que tener ya los ejemplos cargados con la misma contraseña):

```bash
python TennisPlatformApp/demo/load-demo.py --server daniel@tenis-pi --env /srv/tennis-platform/TennisPlatformApp/.env --db-container tennis-prod-postgres-1 --api https://tenis-pi.tail26506d.ts.net/api/v1 --student-password <la misma>
```

Para quitarlos (en la Pi, con `ssh daniel@tenis-pi` y el contenedor `tennis-prod-postgres-1`):

```bash
docker exec -i tennis-postgres psql -U tennis_platform -d tennis_platform < TennisPlatformApp/demo/remove-demo.sql
```
