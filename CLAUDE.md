# CLAUDE.md — Tennis Platform

Guía de entrada al repositorio. Si abres el proyecto en esta carpeta, empieza por aquí.
El detalle técnico del backend está en `TennisPlatformApp/CLAUDE.md`.

## Qué es

Plataforma web/PWA para que un profesor de tenis gestione alumnos, disponibilidad, clases y
reservas. MVP con un único profesor y una única pista. Roles: `ADMIN`, `TEACHER`, `STUDENT`.

## Cómo se trabaja aquí

**Lee `Documentos/_arquitectura/12-metodologia-trabajo.md` antes de tocar nada.** Define el
proceso acordado y no es opcional. Lo esencial:

**Y lee `Documentos/_arquitectura/15-convenciones-de-codigo.md`**, que fija idioma, estilo de
comentarios, nombres de tests, ramas, commits y uso del utillaje. También es vinculante.

- **No se avanza de fase sin validación explícita del usuario.** No encadenes fases.
- **Código, comentarios y commits en inglés; documentación de arquitectura en español.**
- **Cada fase se desarrolla en su rama y entra por Pull Request** con el CI en verde.
- **Los mensajes de commit siguen Conventional Commits** (`feat`, `fix`, `docs`, `ci`...), con
  un cuerpo que explica el porqué. Los anteriores al 18/09/2026 usan el estilo viejo y no se
  reescriben.
- En las fases de análisis y diseño **no se escribe código**: primero requisitos,
  ambigüedades, decisiones explicadas, contratos y criterios de aceptación.
- **La evidencia se pega, no se afirma.** Una fase no se da por terminada sin la salida real
  del comando, y hay que mirar el contador de tests **saltados**, no solo `BUILD SUCCESS`.
- **Cada fase cierra con su commit.**
- Si detectas una mala decisión anterior, **señálala y propón alternativa**; no la mantengas
  por compatibilidad. Ya ha pasado tres veces y las tres eran correcciones necesarias.
- Al terminar cada bloque grande de trabajo, entrega un **resumen de las decisiones tomadas y
  su porqué**. No narres cada archivo mientras trabajas.

Los documentos de `Documentos/_arquitectura/` son vinculantes, no lectura de fondo.

## Estado por fases

| Fase | Estado |
|---|---|
| 1. Análisis funcional | Completada |
| 2. Arquitectura | Completada |
| 3. Modelo de datos | Completada |
| 4. Skeleton del backend | Completada y verificada |
| 5. Seguridad y autenticación (`identity`) | Completada — salvedades cerradas en la 5.1 |
| 5.1 Integración continua | Completada y verificada (run 35137942307 en verde) |
| 6. Perfiles y gestión de usuarios | Completada — `teacher` (PR #13) y `student` (PR #15) en `main` |
| 7. Disponibilidad del profesor (`availability`) | Completada — PR #17 en `main` |
| 8. Clases (`lesson`) | Completada — PR #20 en `main` |
| **9. Reservas (`booking`)** | **Siguiente**: pendiente de análisis |
| 10 en adelante | Pendientes |

El análisis de la Fase 6 y sus decisiones están en
`Documentos/_arquitectura/16-fase6-analisis-perfiles.md`. Se entregó en dos PRs, `teacher`
primero y `student` después, porque `student` depende de `teacher`. Ese documento recoge además
las decisiones que hubo que cerrar al implementar la segunda entrega.

El análisis de la Fase 7 está en `Documentos/_arquitectura/18-fase7-analisis-availability.md`
(es la Fase 3 de `09-roadmap-implementacion.md`: ese documento numera distinto que esta tabla).
Recoge también las decisiones que hubo que cerrar al implementarla.

La Fase 7 entró con una limpieza que sale de su módulo: el cuerpo del `ProblemDetail` estaba
copiado en los cuatro `@RestControllerAdvice` y ahora lo construye `error/Problems.java`, y la
fontanería HTTP de los tests de API vive en `AbstractIntegrationTest` en vez de estar repetida
en cada clase. **Una excepción de dominio nueva se mapea llamando a `Problems.of`**, y un test
de API nuevo hereda `rest`, `bearer`, `jsonBearer`, `tokenOf` y `tokenOfANewStudent` en lugar de
copiarlos.

La exclusión de `EI_EXPOSE_REP2` de `spotbugs-exclude.xml` sigue acotada a las cuatro clases de
`availability`, y su justificación explica la medición que hay detrás. **La Fase 8 confirmó que
acotarla era lo correcto**: los cuatro servicios de `lesson` guardan sus puertos igual y SpotBugs
no los marca, así que el disparador iba con aquellos dos tipos concretos y no con el patrón.

El análisis de la Fase 8 está en `Documentos/_arquitectura/19-fase8-analisis-lesson.md`, con las
cinco decisiones que hubo que cerrar antes de escribir código.
Su informe de cierre está en `Documentos/_informes/informe-fase8-lesson-2026-09-20.md`. La Fase 8
aplazó a la 9 todo lo que depende de contar reservas: `bookedCount`, el estado `FULL`, el marcado
de asistencia y la cascada al cancelar una clase. **`booking` hereda esas cuatro piezas.**

La entrega `student` trae un **módulo nuevo, `platform`**, dueño de la configuración global.
`02-arquitectura.md` asignaba `platform_configuration` a `administration`, y era un error de
propiedad: `student` tiene que leer el límite de alumnos y no puede depender de
`administration`. De momento solo existe el lado de lectura; la consola que la escribe llega
con `administration`, bastante más adelante.

Las dos salvedades que arrastraba la Fase 5 están cerradas: `TeacherBootstrapIdempotencyTest`
demuestra que ejecutar el bootstrap dos veces no crea una segunda cuenta, y el stack se
reconstruyó y arrancó contra el código actual. Las decisiones de la 5.1 están en
`Documentos/_arquitectura/14-fase5.1-integracion-continua.md`.

### `main` no se puede proteger, y hay que trabajar con ello

La API de GitHub responde `403: Upgrade to GitHub Pro or make this repository public` a
cualquier intento de proteger la rama: **la protección de ramas no existe en repositorios
privados con el plan Free**. Nada impide fusionar un PR con el CI en rojo, y el 18/09/2026 pasó
exactamente eso: los PRs automáticos #5 (Spring Boot 4.1.1) y #1 (JDK 26) se fusionaron en rojo
y dejaron `main` sin compilar.

Mientras el repositorio siga privado en Free, la defensa es doble y ninguna de las dos bloquea
de verdad:

- **Dependabot ya no propone versiones mayores** (salvo en GitHub Actions, donde el propio
  pipeline es la prueba completa del cambio). Las mayores se deciden con su rama y su análisis.
- **Hay un hook `pre-push`** en `.githooks/` que rechaza el push directo a `main`. Hay que
  activarlo una vez por clon: `git config core.hooksPath .githooks`.

**Antes de fusionar cualquier PR, mira el check.** Es lo único que queda entre un merge y un
`main` roto.

### Dependabot: qué pasó con los primeros PRs

**Ya no queda ninguno abierto.** La tabla que había aquí describía la situación del 18/09/2026 y
llevaba desde entonces sin corresponderse con la realidad. Resumen de cómo acabó, que es lo
único que sigue siendo útil:

- Los dos que subían versión mayor —Spring Boot a 4.1.1 y la imagen de maven a temurin-26— se
  fusionaron en rojo y rompieron `main`. Los revirtió el PR #7.
- Los que cambiaban PostgreSQL y el JDK del contenedor se cerraron sin fusionar: cambian el
  motor de datos o el runtime, y eso se decide con su rama, no se fusiona por estar en verde.
- El PR #8 configuró `dependabot.yml` para que no vuelva a proponer versiones mayores, y el #12
  ancló por nombre la imagen de maven, cuyo tag (`3.9-eclipse-temurin-21`) esconde el JDK en el
  sufijo y se colaba por la regla general.
- Las actualizaciones menores y de parche sí entraron (PR #11). El backend está hoy en Spring
  Boot 3.5.16 sobre JDK 21.

El criterio queda en pie: **una versión mayor es una decisión con su rama y su análisis, nunca
un merge**, y antes de fusionar cualquier PR hay que mirar el check.

## Comandos

```
cd TennisPlatformApp/backend
mvn verify                # lo mismo que ejecuta CI: Spotless, tests, SpotBugs y JaCoCo
mvn test                  # solo la suite; revisa el contador de "Skipped"
mvn spotless:apply        # corrige el formato que Spotless rechaza
mvn clean package
```

El pipeline (`.github/workflows/ci.yml`) **falla si algún test se salta**, no solo si alguno
rompe: un test de integración saltado parece verde y no prueba nada.

```
cd TennisPlatformApp
docker compose up postgres    # solo la base, para ejecutar el backend desde el IDE
docker compose up -d --build  # stack completo
```

## Entorno de la máquina de desarrollo

- **El backend escucha en el puerto 8081**, no en el 8080: ese está ocupado por un Tomcat 8
  ajeno al proyecto. Configurado en `TennisPlatformApp/.env`.
- `JAVA_HOME` apunta al JDK 21; `java` en el PATH también es el 21.
- Buzón de correo de desarrollo (Mailpit) en **http://localhost:8025**. Ahí aparecen los
  emails de verificación y recuperación.
- PostgreSQL en `localhost:5432`, base/usuario/contraseña `tennis_platform`.

## Trampas conocidas

Cosas que ya han costado tiempo y que fallan **en silencio**:

- **`testcontainers.version` está fijada en `pom.xml`** por encima de la del BOM de Spring
  Boot. Docker Engine 29 rechaza las versiones antiguas de su API, y el síntoma no es un
  fallo: los tests de integración se **saltan** mientras el build sigue en verde.
- **`AbstractIntegrationTest` usa el patrón singleton container** (se arranca una vez y no se
  para nunca; lo limpia Ryuk). Cambiarlo a `@Container` apaga la base al terminar la primera
  clase de test y todas las siguientes fallan con `Failed to obtain JDBC Connection`.
- **`out` es un paquete de código**, no un directorio de compilación: `application/port/out` y
  `adapters/out`. Por eso `.gitignore` ancla esa regla a la raíz. Una regla `out/` suelta
  excluye medio módulo de cada commit sin avisar.
- **El rate limiting está subido en el perfil de test.** Todos los tests llaman desde
  `127.0.0.1` y se estrangularían entre ellos. `AuthRateLimitTest` lo baja y además desactiva
  los reintentos del cliente HTTP, porque Apache HttpClient respeta `Retry-After` y reintenta
  el 429 cuando el margen ya se ha repuesto.
- **Revocar y lanzar excepción en el mismo método transaccional deshace la revocación.** Pasó
  con la detección de reutilización de refresh tokens: hace falta `noRollbackFor`.
- **`AbstractIntegrationTest` vacía la base antes de cada test** (`TRUNCATE ... CASCADE`).
  Una tabla nueva hay que añadirla a esa lista, o sus filas sobrevivirán entre tests y el
  resultado volverá a depender del orden. Y un test que necesite al profesor debe sembrarlo:
  lo que creó el bootstrap al arrancar el contexto ya no está.
- **El perfil del profesor lo crea el bootstrap, no un endpoint.** `display_name` y `timezone`
  son `NOT NULL` y no hay alta pública de profesor: sin `TEACHER_EMAIL` y `TEACHER_PASSWORD`
  en el entorno no existe profesor, y `GET /teacher/profile` responde 404.
- **`TRUNCATE ... CASCADE` vacía `platform_configuration` aunque no esté en la lista.** Llega
  hasta ella por la clave ajena `updated_by` → `users`. Por eso la limpieza de
  `AbstractIntegrationTest` vuelve a sembrar la fila: sin eso, a partir del segundo test el
  límite de alumnos sería el de reserva del código en vez del configurado, y el síntoma sería
  un número raro en una aserción que no tiene nada que ver.
- **El `CsrfFilter` va *antes* del `ExceptionTranslationFilter`.** Configurar
  `exceptionHandling().accessDeniedHandler(...)` no alcanza a lo que rechaza el filtro de CSRF:
  ese 403 se le escapa y cae en la página de error del contenedor. Hay que ponerle el handler
  al propio `CsrfFilter` con un `ObjectPostProcessor`, y así lo hace `SecurityConfig`. El
  síntoma de deshacerlo no es un fallo, es un 403 con otro formato que parece un bug arreglado.
- **`day_of_week` es ISO-8601 (1 = lunes … 7 = domingo)**, no 0-6 como decía
  `10-diagrama-er.md`. Hay tres convenciones cruzadas —`java.time` numera el lunes 1,
  `EXTRACT(DOW)` de PostgreSQL numera el domingo 0, el borrador de la API decía 0 = lunes— y
  elegir mal no falla: mueve el horario un día. La API expone el **nombre**, y el número no
  sale del adaptador de persistencia.
- **La disponibilidad es hora de pared, no instantes.** Solo la resuelve
  `AvailabilitySchedule`, con la zona del profesor. `lesson` y `calendar` preguntan por
  `QueryAvailability` en vez de leer reglas, para que no haya tres implementaciones de una
  misma regla. Las fechas de cambio de hora están escritas literales en los tests: un test que
  le pregunta a `java.time` cuándo cambia la hora se da la razón a sí mismo.
- **El estado de una clase no se guarda entero.** La columna `lessons.status` sólo tiene `OPEN`
  y `CANCELLED`; `COMPLETED` se deduce del reloj al leer y `FULL` del número de reservas, que
  sólo `booking` puede contar. Añadir `FULL` a la columna obligaría a que `booking` lo escribiera
  y lo mantuviera en paz con el recuento real: el día que discrepen, la clase no falla, miente.
  Por lo mismo no hay `cancelled_within_window` — es `cancelled_at` restado de `starts_at`.
- **La duración de una clase se mide en instantes, no en el reloj del profesor.** Los dos días
  del año en que cambia la hora, una clase que en su reloj va de 01:30 a 03:30 dura una hora de
  verdad. Manda el instante, y es el frontend quien debe enseñar la duración resultante antes de
  confirmar. «No cruza medianoche», en cambio, **sí** se evalúa en hora local: medianoche es una
  idea local, y por eso esa regla no puede ser un `CHECK` de la base.
- **La restricción de solapamiento de clases necesita `btree_gist`**, que crea el changeset
  `v6-lesson`. Sin la extensión, la restricción no se puede ni crear. Y se comprueba dos veces a
  propósito: antes en la aplicación para poder responder un 409 con sentido, y en la base porque
  entre la comprobación y el `INSERT` cabe otra petición.
- **El perfil del alumno nace en `PATCH /me`, no en el registro.** Un alumno recién verificado
  tiene los campos personales a `null` en `GET /me`, y eso es correcto: es la señal de que el
  frontend debe pedírselos. Además, un alumno sin perfil **no puede ser asociado** por el
  profesor: `teacher_students` referencia a `student_profiles`, y la respuesta es un 422.

## Repositorio

`https://github.com/SFDK1990/tennis-platform` (privado). La rama de trabajo es `main`.
