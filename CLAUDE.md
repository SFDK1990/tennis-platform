# CLAUDE.md — Tennis Platform

Plataforma web/PWA para que un profesor de tenis gestione alumnos, disponibilidad, clases y
reservas. MVP con un profesor y una pista. Roles: `ADMIN`, `TEACHER`, `STUDENT`.

El detalle técnico del backend está en `TennisPlatformApp/CLAUDE.md`.

## Cómo se trabaja

Vinculantes, y hay que leerlos antes de tocar nada:

- `Documentos/_arquitectura/12-metodologia-trabajo.md` — el proceso y **el estado de las fases**
  (es el único sitio que lo dice).
- `Documentos/_arquitectura/15-convenciones-de-codigo.md` — idioma, comentarios, tests, commits.

Lo esencial: no se avanza de fase sin validación de Daniel; análisis antes de código; una rama y
un PR por fase con el CI en verde; Conventional Commits; código y commits en inglés,
documentación en español; la evidencia se pega (mirando los tests **saltados**); una decisión
vive en un solo sitio. Al terminar un bloque grande, un resumen de decisiones y su porqué.

**`main` no se puede proteger** (repositorio privado en el plan Free). Antes de fusionar, mirar
el check. Hay un hook `pre-push` que rechaza el push directo a `main`; se activa una vez por
clon con `git config core.hooksPath .githooks`. Dependabot no propone versiones mayores: una
mayor se decide con su rama y su análisis.

## Comandos

```
cd TennisPlatformApp/backend
mvn verify                # lo que ejecuta CI: Spotless, tests, SpotBugs, JaCoCo
mvn spotless:apply        # corrige el formato

cd TennisPlatformApp
docker compose up postgres    # sólo la base
docker compose up -d --build  # stack completo

cd TennisPlatformApp/frontend
npm run dev                   # http://localhost:3000, reenvía /api al backend
npm run lint                  # incluye las fronteras entre módulos
npm run typecheck && npm test && npm run build
npm run api:types             # regenera los tipos tras tocar openapi.yaml
```

CI falla si algún test se salta. Sin Docker en marcha, los de integración se saltan.

## Entorno de desarrollo

- Backend en el puerto **8081** (el 8080 lo ocupa un Tomcat ajeno). Ver `TennisPlatformApp/.env`.
- Frontend en el **3000** (Node 24). JDK 21. Mailpit (correos de verificación) en http://localhost:8025.
- Para recorrer pantallas, Playwright CLI (`playwright-cli`, skill en `.claude/skills/`).
- PostgreSQL en `localhost:5432`, base/usuario/contraseña `tennis_platform`.

## Trampas que fallan en silencio

- **`testcontainers.version` está fijada en `pom.xml`**: con la del BOM, Docker Engine 29 hace
  que los tests de integración se **salten** con el build en verde.
- **`AbstractIntegrationTest` usa un contenedor singleton.** Con `@Container` la base se apaga
  tras la primera clase de test.
- **`AbstractIntegrationTest` vacía la base antes de cada test.** Una tabla nueva va a la lista
  del `TRUNCATE`. El `CASCADE` vacía también `platform_configuration`, y por eso se vuelve a
  sembrar. Un test que necesite al profesor lo siembra él.
- **`out` es un paquete de código** (`port/out`, `adapters/out`): la regla de `.gitignore` está
  anclada a la raíz.
- **El rate limiting está subido en el perfil de test**; `AuthRateLimitTest` lo baja.
- **Revocar y lanzar excepción en el mismo método transaccional deshace la revocación**: hace
  falta `noRollbackFor`.
- **El profesor lo crea el bootstrap** con `TEACHER_EMAIL` y `TEACHER_PASSWORD`; sin ellos,
  `GET /teacher/profile` da 404.
- **El `CsrfFilter` va antes del `ExceptionTranslationFilter`**: su handler se pone en el propio
  filtro (`SecurityConfig`), o el 403 sale con otro formato.
- **`day_of_week` es ISO-8601 (1 = lunes).** La API expone el nombre; el número no sale del
  adaptador.
- **La disponibilidad es hora de pared.** Sólo la resuelve `AvailabilitySchedule`; los demás
  preguntan a `QueryAvailability`. Las fechas de cambio de hora van literales en los tests.
- **La duración de una clase se mide en instantes; "no cruza medianoche", en hora local.**
- **`lessons.status` sólo guarda `OPEN`/`CANCELLED`**; `FULL` y `COMPLETED` se derivan al leer.
- **Los solapamientos se comprueban dos veces**: en la aplicación, para dar un 409 útil, y con
  `EXCLUDE USING gist` (necesita `btree_gist`), porque entre la comprobación y el `INSERT` cabe
  otra petición.
- **La última plaza la protege `SELECT ... FOR UPDATE`** en `LockLesson`, que exige transacción.
  `LastSeatConcurrencyTest` falla si se quita el cerrojo; se comprobó quitándolo.
- **La asistencia tiene su columna**, `bookings.attendance`. Mezclarla con `status` sacaría la
  reserva de todo filtro `CONFIRMED` sin fallar.
- **El login acepta cuentas sin verificar**; la verificación se exige al reservar. Un test que
  verifique a un alumno tiene que volver a iniciar sesión, porque el dato va en el token.
- **`:param is null or ...` con un UUID falla en PostgreSQL.** Los filtros opcionales, con
  `Specification`.
- **El perfil del alumno nace en `PATCH /me`**, y sin perfil el profesor no puede gestionarlo.
- **Con sesiones sin estado, la estrategia CSRF por defecto borra la cookie `XSRF-TOKEN` en cada
  petición con bearer**, y el logout siguiente falla con 403 dejando viva la sesión. Por eso
  `SecurityConfig` pone `NullAuthenticatedSessionStrategy`; no quitarlo.
- **Los tipos del frontend se generan de `openapi.yaml`** y se versionan. Tocar el contrato sin
  `npm run api:types` rompe el CI; editar `schema.d.ts` a mano, también.
- **No hay admin** hasta la Fase 12: para probar, se registra uno y se le cambia el rol en la base.

## Repositorio

`https://github.com/SFDK1990/tennis-platform` (privado). Rama principal: `main`.
