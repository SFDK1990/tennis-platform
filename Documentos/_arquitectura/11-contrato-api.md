# Tennis Platform — Contrato de API

El contrato es `../../TennisPlatformApp/openapi.yaml` (OpenAPI 3.0). Describe lo que el backend
hace, y eso se comprueba en cada build (Fase 13, `24-fase13-analisis-revision-api.md`):

- cada respuesta de los tests de integración se valida contra el spec: un estado sin documentar,
  un campo que no declara o uno que falta hacen fallar el test (`ContractValidation`);
- las rutas del código y las del spec son el mismo conjunto (`ImplementedRoutesMatchTheContractTest`);
- los estados comunes están en todas las operaciones (`ContractConventionsTest`, ver abajo);
- los tipos del frontend se generan del spec, y el CI falla si no coinciden.

Tocar un endpoint es tocar el spec en el mismo cambio.

## Convenciones generales

- Todas las rutas van bajo `/api/v1`.
- Autenticación: `Authorization: Bearer <accessToken>` en cada petición autenticada. El `accessToken` es de corta duración (ver `02-arquitectura.md`).
- El `refreshToken` viaja en una cookie `HttpOnly`, `Secure`, `SameSite` (decisión ya cerrada); nunca aparece en el cuerpo de ninguna respuesta ni request, salvo el propio `Set-Cookie` que hace el backend en `/auth/login` y `/auth/refresh`.
- **`POST /auth/refresh` y `POST /auth/logout` exigen además la cabecera `X-XSRF-TOKEN`.** Son los dos únicos endpoints que se autentican con la cookie sola, y una cookie la envía el navegador aunque la petición la origine otro sitio. Ver "Protección CSRF" más abajo.
- Paginación en listados (`/teacher/students`, `/bookings`, `/admin/users`): query params `page` (0-index, por defecto 0) y `size` (por defecto 20), respuesta envuelta en `{ items: [...], page, size, totalItems }`.
- `/calendar` no pagina: se filtra por rango de fechas obligatorio (`from`, `to`), acotado a un máximo razonable (p. ej. 62 días) para no exponer una consulta ilimitada.
- Todas las fechas/horas son ISO 8601 (`date-time` en UTC, sufijo `Z`); el frontend convierte a hora local del usuario, incluyendo la zona horaria del profesor cuando aplica (decisión ya cerrada en `00-indice-arquitectura.md`).
- Los errores siempre usan `application/problem+json` (RFC 7807) con el schema `ProblemDetails`, que añade un campo `code` con los códigos de negocio ya definidos en `02-arquitectura.md`.
- **Estados comunes, documentados en cada operación**: `401` en toda la que exige bearer token,
  `429` en todo `/auth/*` (el límite por IP cubre el prefijo entero) y `400` en toda la que recibe
  cuerpo, id o parámetros. Son los que un test rara vez provoca, y por eso los comprueba un test
  sobre el propio spec.
- **Las transiciones de estado son acciones** (`POST .../cancel`, `.../manage`, `.../attendance`,
  `PATCH .../status`), no un `DELETE` ni un `PATCH` genérico: cancelar deja la reserva con su
  motivo, y cada transición tiene reglas propias (ventana de 24 horas, quién la hace).
- **Un recurso se escribe por un solo camino.** El perfil de cualquier rol se cambia con
  `PATCH /me`; `GET /teacher/profile` es de solo lectura.

## Protección CSRF

CSRF está activo **solo** en `POST /auth/refresh` y `POST /auth/logout`. El resto de la API se
autoriza con `Authorization: Bearer`, y una petición cross-site no puede adjuntar esa cabecera:
exigir allí un token CSRF sería ceremonia sin amenaza que cubrir. Esos dos endpoints son la
excepción porque se autentican con la cookie `refresh_token` y nada más, y las cookies las envía
el navegador venga la petición de donde venga.

El mecanismo es el de doble envío:

1. El backend fija una cookie `XSRF-TOKEN` **legible por JavaScript** (no `HttpOnly`, a
   propósito: el cliente tiene que poder leerla) en **toda** respuesta, no solo en las de esos
   dos endpoints. Si solo se fijara donde hace falta, el cliente no tendría nada que enviar en
   su primer `refresh`.
2. El cliente copia ese valor en la cabecera `X-XSRF-TOKEN` de la petición.
3. El backend compara cookie y cabecera. Un formulario de otro sitio consigue que el navegador
   mande la cookie, pero no puede leerla para rellenar la cabecera.

Sin la cabecera, o con un valor que no coincide, la respuesta es `403` con
`code: AUTH_CSRF_TOKEN_INVALID`. **No es un problema de permisos**: el cliente no tiene que
volver a autenticarse, tiene que reenviar la petición con la cabecera puesta. Se documenta aquí
y en `openapi.yaml` (esquema de seguridad `csrfToken`) porque no estarlo ya costó una tarde de
depuración durante las pruebas manuales del 19/09/2026.

## Errores que no los produce un controlador

Cuatro respuestas nacen en la cadena de filtros, antes de que la petición llegue
a ningún controlador, y por tanto fuera del alcance de cualquier `@RestControllerAdvice`. Por
omisión salían con el formato por defecto del contenedor —o con el cuerpo vacío— incumpliendo la
regla de que *todos* los errores son `application/problem+json`. Desde la corrección siguen el
contrato como el resto:

| Código                     | HTTP | Cuándo                                                        |
|-----------------------------|------|----------------------------------------------------------------|
| `AUTH_UNAUTHENTICATED`      | 401  | No hay bearer token, o no es válido, en un endpoint que lo exige |
| `AUTH_CSRF_TOKEN_INVALID`   | 403  | Falta `X-XSRF-TOKEN` o no coincide con la cookie                |
| `AUTH_FORBIDDEN`            | 403  | Denegación de autorización genérica de la cadena de filtros      |
| `AUTH_RATE_LIMITED`         | 429  | Demasiadas peticiones a `/auth/*` desde la misma IP; `Retry-After` dice cuánto esperar |

`AUTH_UNAUTHENTICATED` es deliberadamente vago: no distingue token ausente de expirado, mal
formado o firmado por otro. Esa diferencia es justo lo que un atacante necesita para saber cuál
de sus intentos se acerca.

`AUTH_FORBIDDEN` es el valor por defecto de la cadena y tiene su prueba. Desde la Fase 9 lo
usa también `booking` con el mismo significado —autenticado, pero con un rol al que esa operación
no sirve—: un profesor que intenta reservar plaza, o un admin que pide "mis reservas".

## Códigos comunes a todos los módulos

| Código               | HTTP | Motivo |
|-----------------------|------|--------|
| `VALIDATION_ERROR`    | 400  | Cuerpo inválido, JSON mal formado, parámetro obligatorio ausente o de tipo incorrecto (un id que no es UUID) |
| `DATE_RANGE_INVALID`  | 400  | Rango `from`/`to` ausente, invertido o de más de 62 días. Es el mismo tope para toda consulta por rango |
| `AUTH_FORBIDDEN`      | 403  | Autenticado, pero con un rol al que esa operación no sirve |
| `TEACHER_FORBIDDEN`   | 403  | La operación es del profesor dueño de lo que se toca |

## Mapeo código de negocio → HTTP

No estaba explícito en los documentos previos qué status exacto lleva cada código; se fija aquí para que backend y frontend lo interpreten igual:

| Código                        | HTTP | Endpoint típico                              | Motivo                                                            |
|--------------------------------|------|-----------------------------------------------|--------------------------------------------------------------------|
| `LESSON_FULL`                  | 409  | `POST /lessons/{id}/bookings`                 | El estado leído por el cliente ya no es válido (capacidad agotada) |
| `LESSON_OVERLAP`                | 409  | `POST /teacher/lessons`                       | Choca con otra clase existente del profesor                        |
| `BOOKING_ALREADY_EXISTS`       | 409  | `POST /lessons/{id}/bookings`                 | Ya existe una reserva `CONFIRMED` de ese alumno para esa clase      |
| `STUDENT_SCHEDULE_OVERLAP`     | 409  | `POST /lessons/{id}/bookings`                 | El alumno ya tiene otra reserva confirmada que solapa en horario   |
| `STUDENT_NOT_MANAGED`          | 403  | `POST /lessons/{id}/bookings`                 | El alumno no está gestionado por el profesor                       |
| `CANCELLATION_WINDOW_EXPIRED`  | 422  | `POST /bookings/{id}/cancel`                  | Regla de negocio (ventana de 24h), no un conflicto de concurrencia  |

Códigos añadidos en la Fase 6, con el mismo criterio:

| Código                        | HTTP | Endpoint típico                                  | Motivo                                                               |
|--------------------------------|------|---------------------------------------------------|-----------------------------------------------------------------------|
| `STUDENT_ALREADY_MANAGED`      | 409  | `POST /teacher/students/{userId}/manage`          | Ya existe la relación: el cliente debe releer el estado               |
| `STUDENT_ALREADY_INACTIVE`     | 409  | `DELETE /teacher/students/{userId}/manage`        | Ya estaba desactivado; suele ser una pantalla obsoleta                |
| `STUDENT_LIMIT_REACHED`        | 422  | `POST /teacher/students/{userId}/manage`          | Regla de negocio: el límite de `platform_configuration`               |
| `STUDENT_PROFILE_INCOMPLETE`   | 422  | `POST /teacher/students/{userId}/manage`          | El alumno aún no ha rellenado sus datos; releer no lo cambia          |
| `STUDENT_NOT_FOUND`            | 404  | `POST /teacher/students/{userId}/manage`          | No hay cuenta de alumno con ese id                                    |
| `TEACHER_FORBIDDEN`            | 403  | `/teacher/**`                                     | El llamante no es el profesor                                         |
| `FIELD_NOT_APPLICABLE_TO_ROLE` | 400  | `PATCH /me`                                       | El cuerpo trae un campo de otro rol; se rechaza en vez de ignorarlo   |
| `AUTH_SESSION_EXPIRED`         | 401  | `POST /auth/refresh`                              | Refresh token ausente, expirado, revocado o reusado: los cuatro casos responden idénticamente |
| `AUTH_WEAK_PASSWORD`           | 400  | `POST /auth/register`, `POST /auth/reset-password` | Más de 72 bytes: bcrypt ignoraría el resto (Fase 14)                  |
| `AUTH_ACCOUNT_NOT_ACTIVE`      | 403  | `POST /auth/verify-email`, `POST /auth/reset-password` | Cuenta desactivada después de enviarle el enlace (Fase 14)        |

Códigos añadidos en la Fase 7:

| Código                          | HTTP | Endpoint típico                                   | Motivo                                                                 |
|----------------------------------|------|----------------------------------------------------|-------------------------------------------------------------------------|
| `AVAILABILITY_RULES_OVERLAP`     | 400  | `PUT /teacher/availability/weekly`                 | Dos reglas del mismo día se pisan en el conjunto enviado                |
| `AVAILABILITY_INVALID`           | 400  | `PUT /teacher/availability/weekly`, `POST .../exceptions` | Día desconocido, fin no posterior al inicio, `EXTRA` sin horas   |
| `AVAILABILITY_EXCEPTION_NOT_FOUND` | 404 | `DELETE /teacher/availability/exceptions/{id}`     | No existe; un id de otro profesor responde lo mismo                     |

Códigos añadidos en la Fase 8:

| Código                        | HTTP | Endpoint típico                        | Motivo                                                                      |
|--------------------------------|------|-----------------------------------------|------------------------------------------------------------------------------|
| `LESSON_OVERLAP`               | 409  | `POST /teacher/lessons`                 | Choca con otra clase no cancelada. Es un 409 porque la otra puede cancelarse un segundo después |
| `LESSON_OUTSIDE_AVAILABILITY`  | 422  | `POST /teacher/lessons`                 | Fuera del horario y sin pedir forzarlo. Releer no cambia nada: o se mueve la clase o se fuerza  |
| `LESSON_INVALID`               | 400  | `POST /teacher/lessons`                 | Duración que no es múltiplo de 30, fin antes del inicio, o cruza medianoche en la zona del profesor |
| `LESSON_ALREADY_CANCELLED`     | 409  | `POST /teacher/lessons/{id}/cancel`     | Ya estaba cancelada; casi siempre una pantalla obsoleta                      |
| `LESSON_ALREADY_FINISHED`      | 422  | `POST /teacher/lessons/{id}/cancel`     | La clase ya terminó. Cancelar lo que ya ocurrió es reescribir el pasado      |
| `LESSON_NOT_FOUND`             | 404  | `/lessons/{id}`                         | No existe; la clase de otro profesor responde lo mismo                       |

`CANCELLATION_WINDOW_EXPIRED` **no** aparece en `POST /teacher/lessons/{id}/cancel`, y es un
cambio deliberado respecto a lo que este documento decía. La ventana de 24 horas protege al
profesor de un hueco que ya no puede llenar, lo que justifica atar al alumno que cancela su
reserva y no justifica atar al profesor sobre su propia clase: tal como estaba escrito, un
profesor que enfermara la noche antes no podía cancelar. El código sigue reservado para
`POST /bookings/{id}/cancel`, y solo cuando quien cancela es el alumno.

Códigos añadidos en la Fase 9 (ver `20-fase9-analisis-booking.md`):

| Código                        | HTTP | Endpoint típico                              | Motivo                                                                  |
|--------------------------------|------|-----------------------------------------------|--------------------------------------------------------------------------|
| `LESSON_NOT_BOOKABLE`          | 409  | `POST /lessons/{id}/bookings`                 | La clase está cancelada; la pantalla que la ofrecía está obsoleta        |
| `LESSON_ALREADY_STARTED`       | 422  | reservar, y cancelar una reserva              | La clase ya empezó. Releer no hace retroceder el reloj                   |
| `LESSON_IN_THE_PAST`           | 422  | `POST /teacher/lessons`                       | La clase empezaría al crearla o antes: no podría tener reservas          |
| `BOOKING_NOT_FOUND`            | 404  | `/bookings/{id}/cancel`, asistencia           | No existe; la reserva de otro responde lo mismo                          |
| `BOOKING_ALREADY_CANCELLED`    | 409  | `POST /bookings/{id}/cancel`, asistencia      | Ya no está en pie; casi siempre una pantalla obsoleta                    |
| `ATTENDANCE_NOT_YET_OPEN`      | 422  | `POST /teacher/lessons/{id}/attendance`       | La asistencia se marca a partir del inicio de la clase                   |
| `EMAIL_NOT_VERIFIED`           | 403  | `POST /lessons/{id}/bookings`                 | El alumno no ha verificado su dirección. Nada antes lo exige: el login sí la acepta |

`LESSON_FULL`, `BOOKING_ALREADY_EXISTS`, `STUDENT_SCHEDULE_OVERLAP`, `STUDENT_NOT_MANAGED` y
`CANCELLATION_WINDOW_EXPIRED` ya estaban en la primera tabla y se implementan tal cual. Un filtro
`status` desconocido en `GET /bookings`, o una misma reserva dos veces en un lote de asistencia,
responden `400 VALIDATION_ERROR`, el código que ya usan los fallos de validación.

Los códigos de la Fase 7 son todos de forma o de referencia; los de la Fase 8 incluyen dos
`422` porque aquí sí hay reglas de negocio sobre datos por lo demás válidos: una clase fuera
del horario está perfectamente bien formada, y una clase que ya terminó también.

Ninguno de los de la Fase 7 es un `422`: todos son errores de forma o de referencia, no
violaciones de una regla de negocio sobre datos por lo demás válidos. `PUT /teacher/availability/weekly` y las dos rutas de
excepciones responden además `403 TEACHER_FORBIDDEN` a quien no sea el profesor, mientras que
`GET /teacher/availability` lo puede leer cualquier autenticado, igual que el perfil del profesor.

### El rango de `GET /teacher/availability`

`from` y `to` son obligatorios, de tipo `date`, y no pueden abarcar más de **62 días** contando
ambos extremos. El rango filtra **solo las excepciones**; las reglas semanales se devuelven
enteras porque son un conjunto pequeño y acotado.

El número es el que este documento sugería para `/calendar` ("p. ej. 62 días"). La Fase 7
convierte esa sugerencia en una decisión, y `/calendar` heredará el mismo tope en su fase: dos
topes distintos para la misma clase de consulta serían peor que cualquiera de los dos.

`STUDENT_NOT_MANAGED` (403), que ya existía para reservas, se usa también cuando el profesor
pide datos de un alumno con el que no tiene relación: la falta de relación es lo que se niega,
tanto al reservar como al leer.

Códigos añadidos en la Fase 12:

| Código | HTTP | Endpoint típico | Motivo |
|---|---|---|---|
| `ADMIN_TARGET_NOT_ALLOWED` | 403 | `PATCH /admin/users/{id}/status` | La cuenta no es de un alumno: ni el profesor ni un admin se desactivan desde la consola |
| `USER_NOT_FOUND` | 404 | `PATCH /admin/users/{id}/status` | No existe una cuenta con ese id |

`409` se reserva para los casos donde el frontend debe releer el estado (calendario desactualizado); `422` para violaciones de regla que no dependen de una carrera de concurrencia; `403` para falta de autorización/relación. Este criterio es el mismo que ya recomendaba `02-arquitectura.md` para tratar las respuestas `409` como "el calendario puede estar obsoleto, vuelve a consultarlo".

## Endpoints cubiertos

32 operaciones, agrupadas por las etiquetas del spec. En la Fase 13 se retiraron
`PATCH /teacher/profile` (duplicaba `PATCH /me`) y `GET /teacher/lessons` (lo sustituyó
`/calendar`).

## Pendiente

- Ejemplos (`examples:`) por endpoint, cuando haya payloads de referencia acordados.
