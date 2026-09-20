# Tennis Platform — Contrato de API

Formaliza la lista de endpoints de `02-arquitectura.md` en un spec OpenAPI 3.0 completo (`../../TennisPlatformApp/openapi.yaml`, en la raíz del proyecto de código — `TennisPlatformApp/`, hermana de esta carpeta `Documentos/`, no dentro de ella — junto a donde ya vive `backend/` e irá `frontend/`), con schemas de request/response reales, para que frontend y backend puedan avanzar en paralelo desde la Fase 1 del roadmap contra un contrato fijo en lugar de contra suposiciones.

Durante la implementación, este archivo puede moverse a `docs/openapi.yaml` dentro del monorepo (o generarse desde las anotaciones de los controllers, si se prefiere code-first más adelante) y debe mantenerse como fuente de verdad versionada junto al código.

## Convenciones generales

- Todas las rutas van bajo `/api/v1`.
- Autenticación: `Authorization: Bearer <accessToken>` en cada petición autenticada. El `accessToken` es de corta duración (ver `08-security-engineer.md`).
- El `refreshToken` viaja en una cookie `HttpOnly`, `Secure`, `SameSite` (decisión ya cerrada); nunca aparece en el cuerpo de ninguna respuesta ni request, salvo el propio `Set-Cookie` que hace el backend en `/auth/login` y `/auth/refresh`.
- **`POST /auth/refresh` y `POST /auth/logout` exigen además la cabecera `X-XSRF-TOKEN`.** Son los dos únicos endpoints que se autentican con la cookie sola, y una cookie la envía el navegador aunque la petición la origine otro sitio. Ver "Protección CSRF" más abajo.
- Paginación en listados (`/teacher/students`, `/bookings`, `/admin/users`): query params `page` (0-index, por defecto 0) y `size` (por defecto 20), respuesta envuelta en `{ items: [...], page, size, totalItems }`.
- `/calendar` no pagina: se filtra por rango de fechas obligatorio (`from`, `to`), acotado a un máximo razonable (p. ej. 62 días) para no exponer una consulta ilimitada.
- Todas las fechas/horas son ISO 8601 (`date-time` en UTC, sufijo `Z`); el frontend convierte a hora local del usuario, incluyendo la zona horaria del profesor cuando aplica (decisión ya cerrada en `00-indice-arquitectura.md`).
- Los errores siempre usan `application/problem+json` (RFC 7807) con el schema `ProblemDetails`, que añade un campo `code` con los códigos de negocio ya definidos en `02-arquitectura.md`.

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

Tres respuestas nacen en la cadena de filtros de Spring Security, antes de que la petición llegue
a ningún controlador, y por tanto fuera del alcance de cualquier `@RestControllerAdvice`. Por
omisión salían con el formato por defecto del contenedor —o con el cuerpo vacío— incumpliendo la
regla de que *todos* los errores son `application/problem+json`. Desde la corrección siguen el
contrato como el resto:

| Código                     | HTTP | Cuándo                                                        |
|-----------------------------|------|----------------------------------------------------------------|
| `AUTH_UNAUTHENTICATED`      | 401  | No hay bearer token, o no es válido, en un endpoint que lo exige |
| `AUTH_CSRF_TOKEN_INVALID`   | 403  | Falta `X-XSRF-TOKEN` o no coincide con la cookie                |
| `AUTH_FORBIDDEN`            | 403  | Denegación de autorización genérica de la cadena de filtros      |

`AUTH_UNAUTHENTICATED` es deliberadamente vago: no distingue token ausente de expirado, mal
formado o firmado por otro. Esa diferencia es justo lo que un atacante necesita para saber cuál
de sus intentos se acerca.

`AUTH_FORBIDDEN` no lo produce hoy ningún endpoint —los 403 de negocio los lanzan los módulos y
los mapea su propio advice—, pero es el valor por defecto de la cadena y tiene su prueba.

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

Códigos añadidos en la Fase 7:

| Código                          | HTTP | Endpoint típico                                   | Motivo                                                                 |
|----------------------------------|------|----------------------------------------------------|-------------------------------------------------------------------------|
| `AVAILABILITY_RULES_OVERLAP`     | 400  | `PUT /teacher/availability/weekly`                 | Dos reglas del mismo día se pisan en el conjunto enviado                |
| `AVAILABILITY_INVALID`           | 400  | `PUT /teacher/availability/weekly`, `POST .../exceptions` | Día desconocido, fin no posterior al inicio, `EXTRA` sin horas   |
| `AVAILABILITY_RANGE_TOO_WIDE`    | 400  | `GET /teacher/availability`                        | El rango pedido supera los 62 días                                      |
| `AVAILABILITY_EXCEPTION_NOT_FOUND` | 404 | `DELETE /teacher/availability/exceptions/{id}`     | No existe; un id de otro profesor responde lo mismo                     |

Códigos añadidos en la Fase 8:

| Código                        | HTTP | Endpoint típico                        | Motivo                                                                      |
|--------------------------------|------|-----------------------------------------|------------------------------------------------------------------------------|
| `LESSON_OVERLAP`               | 409  | `POST /teacher/lessons`                 | Choca con otra clase no cancelada. Es un 409 porque la otra puede cancelarse un segundo después |
| `LESSON_OUTSIDE_AVAILABILITY`  | 422  | `POST /teacher/lessons`                 | Fuera del horario y sin pedir forzarlo. Releer no cambia nada: o se mueve la clase o se fuerza  |
| `LESSON_INVALID`               | 400  | `POST /teacher/lessons`                 | Duración que no es múltiplo de 30, fin antes del inicio, o cruza medianoche en la zona del profesor |
| `LESSON_RANGE_TOO_WIDE`        | 400  | `GET /teacher/lessons`                  | El rango pedido supera los 62 días, el mismo tope que la disponibilidad      |
| `LESSON_ALREADY_CANCELLED`     | 409  | `POST /teacher/lessons/{id}/cancel`     | Ya estaba cancelada; casi siempre una pantalla obsoleta                      |
| `LESSON_ALREADY_FINISHED`      | 422  | `POST /teacher/lessons/{id}/cancel`     | La clase ya terminó. Cancelar lo que ya ocurrió es reescribir el pasado      |
| `LESSON_NOT_FOUND`             | 404  | `/lessons/{id}`                         | No existe; la clase de otro profesor responde lo mismo                       |

`CANCELLATION_WINDOW_EXPIRED` **no** aparece en `POST /teacher/lessons/{id}/cancel`, y es un
cambio deliberado respecto a lo que este documento decía. La ventana de 24 horas protege al
profesor de un hueco que ya no puede llenar, lo que justifica atar al alumno que cancela su
reserva y no justifica atar al profesor sobre su propia clase: tal como estaba escrito, un
profesor que enfermara la noche antes no podía cancelar. El código sigue reservado para
`POST /bookings/{id}/cancel` en la Fase 9.

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

`409` se reserva para los casos donde el frontend debe releer el estado (calendario desactualizado); `422` para violaciones de regla que no dependen de una carrera de concurrencia; `403` para falta de autorización/relación. Este criterio es el mismo que ya recomendaba `04-frontend-engineer-next-react.md` para tratar las respuestas `409` como "el calendario puede estar obsoleto, vuelve a consultarlo".

## Endpoints cubiertos

El spec cubre los 24 endpoints ya listados en `02-arquitectura.md` §8, agrupados en los mismos cuatro bloques: `auth`/`me`, `teacher`/`students`, `availability`, `lesson`/`booking`/`calendar`, `administration`. Ver `openapi.yaml` para el detalle de schemas, parámetros y respuestas de error de cada uno.

## Pendiente al pasar a implementación

- Generar el cliente TypeScript tipado del frontend a partir de este spec (p. ej. `openapi-typescript`), en vez de escribir los tipos a mano, para que un cambio de contrato rompa la build en vez de fallar en runtime.
- Añadir ejemplos (`examples:`) por endpoint una vez haya payloads reales de referencia acordados con QA.
- Revisar si `PATCH /me` necesita separarse en endpoints específicos por rol (`/teacher/profile` y perfil de alumno ya son independientes) para evitar un DTO demasiado genérico; se deja como está por ahora porque el propio `02-arquitectura.md` ya lo definía así. **Actualización de la Fase 6**: se mantiene el DTO único, pero deja de ser permisivo — un campo que no corresponde al rol se rechaza con `400` en lugar de ignorarse, así que el DTO es genérico en la forma pero no en el comportamiento.
