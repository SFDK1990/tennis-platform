# Informe — rama `fix/csrf-contract-and-problem-json` (20/09/2026)

Cierra los pendientes 1 y 2 del `handoff-siguiente-sesion.md`: los dos hallazgos del arranque
local del 19/09 y la línea de `CLAUDE.md` que se había quedado atrás. Fichero de trabajo, no
versionado.

## Qué se ha hecho

### Hallazgo 2 — los errores de la cadena de seguridad no seguían el contrato

`11-contrato-api.md` promete que **todos** los errores son `application/problem+json` con un
campo `code`. Tres respuestas no lo eran, y las tres nacen fuera de Spring MVC, donde ningún
`@RestControllerAdvice` llega:

| Respuesta | Antes | Ahora |
|---|---|---|
| 401 sin token válido | cuerpo vacío | `problem+json`, `code: AUTH_UNAUTHENTICATED` |
| 403 por CSRF | página de error del contenedor (`{"timestamp":…}`) | `problem+json`, `code: AUTH_CSRF_TOKEN_INVALID` |
| 403 de autorización genérica | página de error del contenedor | `problem+json`, `code: AUTH_FORBIDDEN` |

El paquete `com.tennisplatform.error` —que `ModuleBoundariesTest` ya tenía declarado como "el
mapeo global de errores" y exento del grafo de módulos— gana tres clases: `ProblemDetailWriter`,
`ProblemDetailAuthenticationEntryPoint` y `ProblemDetailAccessDeniedHandler`.

**La decisión menos obvia del cambio**: el handler de CSRF no se puede instalar con
`exceptionHandling().accessDeniedHandler(...)`. Esa configuración cablea el
`ExceptionTranslationFilter`, que va **después** del `CsrfFilter` en la cadena y por tanto nunca
ve lo que este rechaza. El 403 de CSRF seguiría cayendo en la página de error del contenedor
mientras todos los demás cumplen el contrato — el peor resultado posible, porque la
inconsistencia parecería un bug ya arreglado. Se le pone el handler al propio `CsrfFilter` con
un `ObjectPostProcessor`, ya que `CsrfConfigurer` no expone el setter. Queda anotado como trampa
en `CLAUDE.md`, porque deshacerlo no rompe nada: solo cambia el formato de una respuesta.

`ProblemDetailWriter` usa el **mismo `ObjectMapper` que el resto de la API**, inyectado, no uno
nuevo. Un `ObjectMapper` recién construido no lleva el mixin de `ProblemDetail` y anida los
campos extra bajo `properties`: el `code` existiría pero en otro sitio. El test unitario lo
construye con `Jackson2ObjectMapperBuilder.json()` por la misma razón, para que la aserción
pruebe la forma real y no pase por casualidad.

### Hallazgo 3, encontrado al documentar — los dos 401 de `/auth/refresh` no eran iguales

El javadoc de `POST /auth/refresh` promete que "cookie ausente, expirada, revocada y reusada se
ven idénticas desde fuera". Era cierto del status y **falso del cuerpo**: la cookie ausente
devolvía un `ProblemDetail` sin `code` (vía `ResponseStatusException`) y la cookie muerta un 401
con el cuerpo vacío. Un atacante distinguía los dos casos, que es justo lo que la promesa quería
evitar.

Ahora los dos caminos pasan por el mismo método privado: 401, `problem+json`,
`code: AUTH_SESSION_EXPIRED`, y la cookie limpiada en ambos. El test
`aMissingSessionAndADeadOneAreIndistinguishable` compara los dos cuerpos byte a byte y de paso
demuestra que rechazar la cookie la borra.

### Hallazgo 1 — CSRF no estaba documentado

- `openapi.yaml`: esquema de seguridad nuevo `csrfToken` (`apiKey` en la cabecera
  `X-XSRF-TOKEN`), aplicado a `/auth/refresh` y `/auth/logout`, más una respuesta reutilizable
  `CsrfTokenMissing` para el 403.
- Al aplicarlo salió otra cosa mal documentada: **`/auth/logout` figuraba como autenticado por
  bearer token y respondiendo 401**, y no es ni lo uno ni lo otro. Lee solo la cookie y responde
  204 siempre, incluso sin sesión. Corregido.
- `11-contrato-api.md`: dos secciones nuevas, "Protección CSRF" (por qué solo en esos dos
  endpoints y cómo funciona el doble envío) y "Errores que no los produce un controlador" (la
  tabla de los tres códigos de arriba), más `AUTH_SESSION_EXPIRED` en la tabla de códigos.
- `TennisPlatformApp/CLAUDE.md`: dos párrafos en "API and error conventions".

### Pendiente 2 y documentación desfasada

- **Fase 6**: la tabla de `CLAUDE.md` decía "En curso: `student` en PR". Ahora dice completada,
  con los dos PRs, y apunta que lo siguiente es la Fase 7 (`availability`), avisando de la
  discrepancia de numeración con `09-roadmap-implementacion.md`.
- **Dependabot**: la sección "PRs abiertos y sin revisar" listaba cinco PRs. **No queda ninguno
  abierto**: dos se fusionaron en rojo y los revirtió el #7, tres se cerraron sin fusionar, y
  las menores entraron en el #11. La tabla se ha sustituido por el resumen de cómo acabó y por
  el criterio, que es lo único que sigue sirviendo. También decía "Spring Boot 3.3.5"; el
  backend está hoy en **3.5.16 sobre JDK 21**.
- **Trampa nueva**: el orden `CsrfFilter` → `ExceptionTranslationFilter`, explicado arriba.

## Evidencia

`mvn verify` completo, lo mismo que ejecuta CI:

```
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotbugs:4.10.4.1:check (default) @ backend ---
[INFO] BugInstance size is 0
[INFO] Error size is 0
[INFO] No errors/warnings found
[INFO] --- jacoco:0.8.15:report (report) @ backend ---
[INFO] Analyzed bundle 'backend' with 129 classes
[INFO] BUILD SUCCESS
[INFO] Total time:  02:38 min
```

**Saltados: 0**, que es el contador que importa: un test de integración saltado parece verde y
no prueba nada. Siete tests nuevos.

Tests nuevos:

| Test | Qué demuestra |
|---|---|
| `anUnauthenticatedRequestAnswersWithTheErrorContract` | 401 en `problem+json` con `code`, `status` e `instance` |
| `refreshingWithoutTheCsrfHeaderSaysSoInTheErrorContract` | el 403 de CSRF nombra la cabecera que falta |
| `loggingOutWithoutTheCsrfHeaderSaysSoInTheErrorContract` | logout se comporta igual |
| `aCsrfRejectionNamesTheHeaderTheCallerIsMissing` | rama CSRF del handler, sin servidor |
| `anyOtherDenialIsReportedAsAPlainAuthorizationFailure` | rama genérica: `AUTH_FORBIDDEN` |
| `aCommittedResponseIsLeftAlone` | una respuesta ya enviada no se reescribe |
| `aMissingSessionAndADeadOneAreIndistinguishable` | los dos 401 de refresh son idénticos |

### CI del PR #16

Los dos checks en verde (run 35496641895):

```
Backend build, tests and static analysis   pass   2m10s
Image builds and the stack starts          pass   1m37s

[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.tennisplatform.error.SecurityErrorContractTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.tennisplatform.error.ProblemDetailAccessDeniedHandlerTest
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0 -- in com.tennisplatform.architecture.ModuleBoundariesTest
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] BugInstance size is 0
```

El paso "Fail if any test was skipped" pasó, así que los tests de integración corrieron de
verdad en el runner y no se saltaron por falta de Docker.

## Revisión de seguridad

Lanzada por tocar autenticación. **Sin hallazgos.** Lo comprobado y descartado, resumido:

- **Fuga de información en los cuerpos 401/403**: los mensajes son genéricos a propósito; no
  distinguen token ausente de caducado ni de mal firmado, y no llevan email, id ni estado de
  cuenta. El 403 de CSRF solo explica el mecanismo de doble envío, que no es secreto.
- **Distinción de estados del refresh token**: el cambio *mejora* la situación, no la empeora.
  Antes las dos ramas eran distinguibles por el cuerpo **y** por el `Set-Cookie` (la rama "sin
  cookie" ni siquiera la limpiaba). Ahora son idénticas byte a byte.
- **Detección de reutilización y revocación de familia**: intacta. El `catch` es el mismo, la
  revocación ocurre en `RefreshSessionService` antes de lanzar, y el `noRollbackFor` que la hace
  efectiva no se ha tocado.
- **`setInstance(request.getRequestURI())`**, que refleja dato del cliente: no va a una
  cabecera (no hay response splitting, y Tomcat rechaza con 400 una línea de petición con
  CR/LF); no hay XSS porque el tipo es `problem+json`, Jackson escapa y `nosniff` se escribe; y
  el URI se devuelve a quien lo mandó, así que no revela nada.
- **El `ObjectPostProcessor`**: no cambia la política. El matcher de CSRF, el repositorio de
  tokens y las reglas de autorización son idénticos; lo único que inyecta es el handler.
- **`response.isCommitted()`**: en la ruta alcanzable no hay nada escrito todavía. Queda una
  diferencia teórica —el handler nuevo no hace `resetBuffer()`, cosa que `sendError()` sí
  hacía—, inexplotable aquí porque haría falta un controlador que escribiera datos sensibles y
  luego lanzara `AccessDeniedException` escapando del `DispatcherServlet`.
- **Logs**: las tres clases nuevas no registran nada.

## Riesgos y deuda

- `AUTH_FORBIDDEN` **no lo produce ningún endpoint hoy**: todos los 403 de negocio los lanzan
  los módulos y los mapea su propio advice. Es el valor por defecto de la cadena. Por eso tiene
  test unitario y no de integración: un valor por defecto que nadie prueba se pudre en silencio.
- Sigue en pie la deuda anotada del handoff: **desactivar un alumno no cancela sus reservas
  futuras** porque `booking` no existe. Tiene fase asignada y criterio de salida.
- El stack local **no se ha vuelto a levantar** contra este código. Los tests de integración
  corren contra PostgreSQL real vía Testcontainers, así que el comportamiento está probado, pero
  si quieres repetir las pruebas manuales del 19/09 hay que reconstruir la imagen
  (`docker compose up -d --build`).

## Qué queda por decidir

1. **Fusionar el PR #16** — https://github.com/SFDK1990/tennis-platform/pull/16. El CI ya está
   en verde; el merge es tuyo.
2. Los tres ficheros sin versionar siguen sin tocar, como pediste. Este informe es un cuarto.
3. Después: **Fase 7, `availability`**, que empieza por su documento de análisis —sin escribir
   código— igual que la Fase 6.
