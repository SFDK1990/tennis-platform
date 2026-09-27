# Fase 16 — Análisis: observabilidad

Criterio de salida (`12-metodologia-trabajo.md`): logs estructurados, métricas de reservas y
conflictos, y un correlation id de punta a punta. La pregunta que tiene que poder contestarse es
la de Marcos: "un alumno dice que reservó y no aparece". Con un id o una hora, el log tiene que
decir qué pasó, a quién y por qué, sin exponer datos personales.

## Punto de partida

- **Correlation id**: ya existe en el backend. `CorrelationIdFilter` reutiliza el
  `X-Correlation-Id` que llega si es seguro (sólo `[A-Za-z0-9._:-]`, hasta 64 caracteres), o
  genera uno. Lo pone en el MDC y en la respuesta. El frontend no lo envía ni lo lee.
- **Logs**: el formato es de texto (`%d %level [correlationId] logger - msg`). Casi no hay
  mensajes: los dos arranques, la reutilización de un refresh token y los 500. Una reserva, una
  cancelación o una clase nueva no dejan rastro.
- **Métricas**: Actuator está, pero sólo expone `health` e `info`. Micrometer ya mide
  `http.server.requests` por ruta y estado, pero nadie puede leerlo. Tampoco sabe distinguir un
  409 `LESSON_FULL` de un `STUDENT_SCHEDULE_OVERLAP`.
- **Health**: `health` es público, sus detalles son sólo para `ADMIN` y hay probes de liveness y
  readiness. No cambia.
- **Spring Boot 3.5** trae los logs estructurados de serie (`logging.structured.format`). No
  hace falta ninguna dependencia.

## Decisiones propuestas

1. **JSON en producción, texto en desarrollo.** El perfil `prod` escribe en formato **ECS** por
   consola (`logging.structured.format.console: ecs`), con los campos del MDC incluidos. Así
   cualquier proveedor de la Fase 17 lo lee sin configurarlo. `dev` y `test` siguen en texto,
   que se lee mejor en una terminal. Logstash también está de serie; ECS es el que nombra los
   campos de forma estándar.
2. **Quién hizo cada petición, en el MDC.** Tras autenticar, un filtro añade `userId` y `role`,
   junto al `correlationId` de ahora. Son UUID y un enum, no datos personales. Así cada línea
   del log dice quién la provocó sin que cada mensaje tenga que repetirlo.
3. **Una línea por petición.** Al terminar cada petición a `/api` se escribe una línea INFO con
   el método, la **plantilla** de la ruta (`/api/v1/lessons/{id}/bookings`, no la ruta con ids),
   el estado, el `code` si fue un error y la duración. No lleva query string ni cuerpo. Es el
   "access log" que hoy no existe, y lo que se busca primero cuando alguien dice "me dio error".
4. **Los cambios de estado dejan una línea INFO**, en el servicio de aplicación que los hace:
   - clase creada o cancelada;
   - reserva creada;
   - reserva cancelada, y por quién (alumno o profesor);
   - asistencia marcada;
   - alumno añadido o quitado;
   - estado de un usuario cambiado por el admin.
   Llevan ids (clase, reserva, alumno), nunca nombres ni emails. Es SLF4J, que las capas de
   aplicación ya usan (`RefreshSessionService`); ArchUnit sólo prohíbe frameworks en el dominio.
5. **Ningún dato personal en el log.** No se escriben email, nombre, teléfono, DNI, contraseña,
   token ni IP (`02-arquitectura.md`). El login fallido y el rate limit se registran sin la
   cuenta ni la IP. **La IP la decide Daniel**: ayuda a ver un abuso, pero es un dato personal y
   obliga a decir cuánto se guarda. La propuesta es dejarla fuera hasta la Fase 17, donde se
   elige el proveedor y la retención.
6. **Las métricas de negocio salen de `http.server.requests`, con el `code` como etiqueta.** No
   se crean contadores dentro de los servicios. Con ruta, estado y código se sabe:
   - reservas hechas: `POST /lessons/{id}/bookings` con 201;
   - conflictos: el mismo, con 409 y `code=LESSON_FULL`, `STUDENT_SCHEDULE_OVERLAP` o
     `BOOKING_ALREADY_EXISTS`;
   - clases que chocan: `POST /teacher/lessons` con `LESSON_OVERLAP`;
   - cancelaciones y las rechazadas por la regla de 24 h (`CANCELLATION_WINDOW_EXPIRED`).
   El `code` llega a la métrica por un atributo de la petición, que ponen los tres sitios que
   escriben un error: las advices (con un `ResponseBodyAdvice` común, no en cada módulo),
   `ProblemDetailWriter` y `AuthRateLimitFilter`. Los códigos son unos cincuenta y fijos, así
   que la etiqueta no dispara la cardinalidad. Sin error, vale `none`.
7. **`/actuator/metrics` sólo para `ADMIN`.** Se expone y la seguridad lo cierra a ese rol, igual
   que los detalles del health. El exportador (Prometheus u otro) depende del proveedor, así que
   se decide en la Fase 17. `/actuator` tampoco pasa por el rewrite de Next, que sólo reenvía
   `/api`.
8. **De punta a punta: el frontend crea el id.** `authenticatedFetch` añade un
   `X-Correlation-Id` (un UUID) a cada petición, y el backend ya lo reutiliza. Un `ApiError` guarda
   el id de la respuesta. Ante un 500 o un fallo sin código conocido, la pantalla dice "Ha fallado
   algo. Código de referencia: `ab12cd34`" (los 8 primeros caracteres). Con eso, Daniel encuentra
   la línea del log. El reintento tras refrescar el token lleva el mismo id, porque es la misma
   acción.
9. **El contrato no cambia.** `X-Correlation-Id` ya está descrito en `11-contrato-api.md`, y una
   cabecera opcional no rompe la validación del spec. No hay endpoints ni campos nuevos.
10. **Sin dependencias nuevas**, en el backend ni en el frontend.

## Fuera, y por qué

- **Dónde se guardan los logs y las métricas, alertas y paneles**: dependen del proveedor, que es
  de la Fase 17.
- **Trazas distribuidas (OpenTelemetry)**: hay un solo servicio detrás de Next. El correlation id
  da lo mismo sin un colector que mantener.
- **Logs del servidor de Next**: Next sólo reenvía `/api` y pinta páginas sin llamar al backend.
  Un fallo de API ya queda en el log del backend.
- **Errores del navegador (tipo Sentry)**: sería otro servicio externo y otra entrada en la CSP.
  Se valora en la Fase 17 si hace falta.
- **Métricas de ocupación o del mes**: son consultas sobre los datos, no métricas. Van con el
  resumen del mes, más adelante.

## Tests

- **Integración, log en JSON**: con el formato ECS activo, una reserva hecha con un
  `X-Correlation-Id` dado deja en la salida la línea de la petición y la del cambio de estado.
  Las dos llevan ese `correlationId` y el `userId` del alumno. Se rompe quitando el filtro del
  MDC.
- **Integración, sin datos personales**: el registro, el login correcto, el fallido y una reserva
  no dejan en la salida el email ni la contraseña usados. Se rompe añadiendo un log con el email.
- **Integración, métricas**: un 409 por la última plaza suma en `http.server.requests` con
  `code=LESSON_FULL`, y un 429 del rate limit con `code=AUTH_RATE_LIMITED`. Se rompe quitando el
  `ResponseBodyAdvice`.
- **Integración, seguridad**: `/actuator/metrics` da 401 sin sesión, 403 a un alumno y al profesor,
  y 200 al admin.
- **Unitarios del frontend**: cada petición sale con `X-Correlation-Id`; el reintento tras el
  refresh conserva el id; `ApiError` lee el id de la respuesta; un 500 muestra el código de
  referencia.

## Criterios de aceptación

- Existe un test que demuestra que, en `prod`, cada línea de una petición es JSON con
  `correlationId` y `userId`.
- Existe un test que demuestra que ni el email ni la contraseña llegan al log en los flujos de
  cuenta y reserva.
- Existe un test que demuestra que un conflicto de reserva se cuenta con su `code`.
- Existe un test que demuestra que `/actuator/metrics` es sólo del admin.
- Existe un test del frontend que demuestra que el id viaja y que un 500 lo enseña.
- Los tres críticos (MDC, `code` en la métrica y ausencia de email) se han visto fallar
  rompiéndolos.
- `mvn verify` y el CI en verde sin tests saltados; `openapi.yaml` y `package.json` sin cambios.

## Orden de trabajo

1. MDC con usuario, línea por petición y JSON en `prod`.
2. Líneas de cambio de estado y revisión de datos personales.
3. `code` en `http.server.requests` y `/actuator/metrics` para el admin.
4. Frontend: id por petición y código de referencia en los errores.
5. Documentación: el formato del log y cómo buscar por id, en `TennisPlatformApp/CLAUDE.md`.

## Decisiones tomadas al implementar

- **La IP queda fuera del log**, como se propuso; se revisa en la Fase 17 con el proveedor y la
  retención.
- **Un paquete `observability`**, fuera de los módulos como `error` y `web`, con la línea por
  petición y la etiqueta de la métrica. En `web` no cabían: `identity` pone el usuario en el MDC,
  y `identity → web → identity` habría sido un ciclo.
- **El mailer ya no escribe la excepción**, sólo su tipo. Un servidor SMTP que rechaza un
  destinatario cita la dirección en el mensaje; el comentario decía que no se registraba y sí se
  hacía.
- **La traza de un 500 se escribe sin los mensajes** (decisión de Daniel en la revisión del PR).
  Un mensaje puede citar lo que recibió la base ("(email)=(...)"). Quedan el tipo de cada
  excepción de la cadena y todas sus líneas de código, que dicen qué falló y dónde; el
  correlation id dice en qué petición. El precio es que algún fallo cueste más de diagnosticar,
  porque el mensaje suele ser la pista.
- **Lo que falla en un filtro también se registra sin mensaje.** Ahí no llega el
  `GlobalExceptionHandler`, y la excepción subía a Tomcat, que la escribe entera: se vio en la
  mutación, con el email en el log. `UnhandledFailureFilter` la recoge antes, la registra como
  un 500 y contesta el mismo `INTERNAL_ERROR`.
- **Hibernate deja de escribir las violaciones de restricción** (`SqlExceptionHelper` en `OFF`).
  Las escribía como ERROR aunque fueran las esperadas (la última plaza, dos clases que se
  pisan, que son un 409), y su mensaje puede citar los valores de la fila. Una que nadie maneja
  sigue llegando al log como excepción no controlada, con su traza.
- **La línea de cambio de estado se escribe antes del commit.** Si el commit fallara, la línea de
  la petición diría 500 con el mismo id; es esa la que confirma el resultado.
- **Una petición que la seguridad rechaza** no llega a un controlador: en la métrica su `uri` es
  `UNKNOWN` y en el log sale la ruta con sus ids. El 429 del rate limit se cuenta igual, con su
  `code`.
- **El id del frontend son 32 caracteres hexadecimales de `crypto.getRandomValues`**, no
  `crypto.randomUUID`, que el navegador sólo da por HTTPS o en `localhost`, y la app también se
  abre por la IP de la red local.
- **El test del JSON reinicia Logback** antes y después de su clase: Logback se configura una vez
  por JVM, y el test pasaba solo y fallaba detrás de cualquier otro.
