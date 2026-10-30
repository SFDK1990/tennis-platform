# CLAUDE.md — Tennis Platform

Plataforma web/PWA para que un profesor de tenis gestione alumnos, disponibilidad, clases y
reservas. Un profesor y dos pistas de tenis. Roles: `ADMIN`, `TEACHER`, `STUDENT`.

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
npm run e2e                   # Playwright contra el stack levantado (ver abajo)
```

CI falla si algún test se salta. Sin Docker en marcha, los de integración se saltan.

`npm run e2e` necesita el stack con el profesor y el admin del `.env`, el rate limiting subido y
las claves del Stripe falso: `docker compose -f compose.yaml -f compose.e2e.yaml up -d --build`.
Construye y arranca el frontend en el puerto 3100 y el Stripe falso en el 12111. Cada test deshace
lo que crea, salvo las cuentas de alumno `e2e-*` y los tipos de bono que alguien compró, que se
retiran.

## Entorno de desarrollo

- Backend en el puerto **8081** (el 8080 lo ocupa un Tomcat ajeno). Ver `TennisPlatformApp/.env`.
- Frontend en el **3000** (Node 24). JDK 21. Mailpit (correos de verificación) en http://localhost:8025.
- Para recorrer pantallas, Playwright CLI (`playwright-cli`, skill en `.claude/skills/`).
- PostgreSQL en `localhost:5432`, base/usuario/contraseña `tennis_platform`.

## Trampas que fallan en silencio

- **Conviven dos Jackson.** Spring Boot 4 usa Jackson 3 (`tools.jackson`); jjwt y el validador
  del contrato, Jackson 2 (`com.fasterxml.jackson`). Para fijar el de ellos por un CVE la
  propiedad es `jackson-2-bom.version`: `jackson-bom.version` es la de Jackson 3.
- **`AbstractIntegrationTest` usa un contenedor singleton.** Con `@Container` la base se apaga
  tras la primera clase de test.
- **Las pistas (`courts`) no se vacían entre tests**: las crea la migración. Un test que las renombre
  deja el nombre como estaba. Una clase guardada sin pista va a la primera.
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
- **La duración de una clase se mide en instantes; "no cruza medianoche", en hora local.** Una
  serie semanal (`WeeklySeries`) suma semanas en hora de pared: siete días de instantes movería
  la clase una hora al cambiar la hora.
- **`lessons.status` sólo guarda `OPEN`/`CANCELLED`**; `FULL` y `COMPLETED` se derivan al leer.
- **Una plaza que se libera pasa a la lista de espera dentro de la misma transacción y bajo el
  cerrojo de la clase** (`WaitingListPromotion`). Un camino nuevo que libere plazas tiene que
  llamarla, o la plaza queda libre para quien llegue; `WaitingListApiTest` tiene un test por camino.
  La lista se cierra 24 horas antes de la clase.
- **El nivel se comprueba en tres sitios**: al reservar, al apuntarse a la lista de espera y en la
  promoción de la lista (`LevelRule`). Un camino nuevo que dé plaza tiene que preguntarle también.
  Un alumno sin nivel reserva cualquier clase.
- **Un nivel en uso no se borra** porque `levels` tiene `ON DELETE RESTRICT` desde alumnos y clases;
  el adaptador traduce la violación a `LEVEL_IN_USE`. Los cuatro niveles iniciales los crea el
  bootstrap: un test que necesite niveles llama a `QueryLevels.seedDefaults`.
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
- **Un `Set-Cookie` puesto en un `ResponseEntity` reemplaza los que ya lleva la respuesta**
  (Spring 7), entre ellos el `XSRF-TOKEN`. Las cookies se añaden a la respuesta del servlet, como
  hace `RefreshTokenCookie`.
- **Con sesiones sin estado, la estrategia CSRF por defecto borra la cookie `XSRF-TOKEN` en cada
  petición con bearer**, y el logout siguiente falla con 403 dejando viva la sesión. Por eso
  `SecurityConfig` pone `NullAuthenticatedSessionStrategy`; no quitarlo.
- **Cada respuesta de un test de integración se valida contra `openapi.yaml`** (`ContractValidation`),
  y un test compara las rutas del código con las del spec. Un endpoint, un campo o un estado nuevo
  sin tocar el spec rompe el build; un nullable junto a `$ref`/`allOf` no hace nada en OpenAPI 3.0.
- **La cobertura del backend es una puerta** (`jacoco:check`). Tras borrar clases, medir con
  `mvn clean verify`: los `.class` viejos de `target/` cuentan como código sin cubrir.
- **Los E2E corren sobre el reloj real.** Lo que depende de la hora (24 horas, clase empezada) lo
  prueba el backend con `Clock` fijo; el test de asistencia crea una clase que empieza a los 15 s.
- **Los tipos del frontend se generan de `openapi.yaml`** y se versionan. Tocar el contrato sin
  `npm run api:types` rompe el CI; editar `schema.d.ts` a mano, también.
- **El frontend lleva una CSP con nonce** (`src/proxy.ts`), y por eso todas las páginas se
  renderizan por petición (`connection()` en el layout raíz). Un `<script>` o un `style={}` en
  línea sin nonce queda bloqueado en silencio: la página carga, pero no se hidrata.
- **La rejilla de la agenda coloca los bloques con clases `row-start-*`/`row-span-*`** que
  genera `@source inline` en `globals.css`: la CSP no deja usar `style`. Sin esa línea, o con
  una fila fuera de su rango, el bloque cae arriba sin error; el E2E del profesor lo comprueba.
- **El rate limiting cuenta por la IP de `X-Forwarded-For`** cuando la petición llega de un proxy
  de confianza (`tennis.identity.trusted-proxies`). En producción, el proxy de entrada tiene que
  sobrescribir esa cabecera, no añadirle: Next pasa tal cual la que manda el cliente.
- **Logback se configura una vez por JVM.** Un test con otro formato de log
  (`logging.structured.format.console`) no lo recibe si otro contexto arrancó antes: pasa solo y
  falla en la suite. `RequestLogTest` reinicia el `LoggingSystem` antes y después de su clase.
- **El código de error llega a la métrica y al log por un atributo de la petición** (`ProblemCode`).
  Quien escriba un error fuera de un `@RestControllerAdvice` tiene que registrarlo, como
  `ProblemDetailWriter` y `AuthRateLimitFilter`, o se contará como `none`.
- **El admin también lo crea el bootstrap**, con `ADMIN_EMAIL` y `ADMIN_PASSWORD`. Sólo cambia el
  estado de alumnos; el profesor y los admins no se desactivan desde la consola.
- **Un alumno borrado no desaparece: su fila se vacía** (`DELETED`, email inventado, perfil «Alumno
  eliminado»), porque sus reservas pasadas la apuntan. Una columna nueva que copie un dato personal
  del alumno lo dejaría tras el borrado: `DeleteMyAccountTest` recorre todas las columnas de texto y
  falla.
- **Un mensaje de Marcos guarda sus destinatarios al enviarse** (`message_recipients`, en
  `notification`): el tablón no se calcula al leer, y quien entra después en un nivel no ve lo
  anterior. Borrar la cuenta saca al alumno de todos por la spi `StudentMessages`.
- **Una foto de alumno no se enlaza con `<img src>` a la API**: no llevaría el token. El frontend la
  pide con `api` y la pinta como data URL (`student_photos`, Fase 25.5).
- **El saldo de un bono no se guarda**: son sus clases menos sus reservas `CONFIRMED` con ese
  `pass_id`. Cancelar por cualquier camino lo devuelve solo; un camino nuevo que *dé* plaza tiene
  que llamar a `ChargePasses.charge` dentro de su transacción (bloquea el saldo), como la reserva y
  la promoción de la lista de espera (Fase 27). Con el interruptor de Marcos apagado no pide nada.
- **El bono pagado online nace con el aviso firmado de Stripe**, nunca con la vuelta del alumno a
  la app (Fase 27.5). La firma se calcula sobre los bytes exactos del cuerpo: el webhook los lee
  como `byte[]`, y un proxy que lo reescriba la rompe. Un aviso repetido no crea otro bono porque
  `checkout_session_id` es único y la compra se bloquea; un importe distinto del de la compra no
  crea ninguno. Ni el CI ni el E2E hablan con Stripe: `FakeStripe` y `e2e/support/fake-stripe.mjs`.
- **El rate limiting cubre también `POST /me/password` y `POST /me/deletion`**, que comprueban una
  contraseña. Una ruta nueva que lo haga tiene que entrar en `AuthRateLimitFilter` y en
  `ContractConventionsTest`.
