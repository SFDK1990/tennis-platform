# Tennis Platform — Contrato de API

Formaliza la lista de endpoints de `02-arquitectura.md` en un spec OpenAPI 3.0 completo (`../../TennisPlatformApp/openapi.yaml`, en la raíz del proyecto de código — `TennisPlatformApp/`, hermana de esta carpeta `Documentos/`, no dentro de ella — junto a donde ya vive `backend/` e irá `frontend/`), con schemas de request/response reales, para que frontend y backend puedan avanzar en paralelo desde la Fase 1 del roadmap contra un contrato fijo en lugar de contra suposiciones.

Durante la implementación, este archivo puede moverse a `docs/openapi.yaml` dentro del monorepo (o generarse desde las anotaciones de los controllers, si se prefiere code-first más adelante) y debe mantenerse como fuente de verdad versionada junto al código.

## Convenciones generales

- Todas las rutas van bajo `/api/v1`.
- Autenticación: `Authorization: Bearer <accessToken>` en cada petición autenticada. El `accessToken` es de corta duración (ver `08-security-engineer.md`).
- El `refreshToken` viaja en una cookie `HttpOnly`, `Secure`, `SameSite` (decisión ya cerrada); nunca aparece en el cuerpo de ninguna respuesta ni request, salvo el propio `Set-Cookie` que hace el backend en `/auth/login` y `/auth/refresh`.
- Paginación en listados (`/teacher/students`, `/bookings`, `/admin/users`): query params `page` (0-index, por defecto 0) y `size` (por defecto 20), respuesta envuelta en `{ items: [...], page, size, totalItems }`.
- `/calendar` no pagina: se filtra por rango de fechas obligatorio (`from`, `to`), acotado a un máximo razonable (p. ej. 62 días) para no exponer una consulta ilimitada.
- Todas las fechas/horas son ISO 8601 (`date-time` en UTC, sufijo `Z`); el frontend convierte a hora local del usuario, incluyendo la zona horaria del profesor cuando aplica (decisión ya cerrada en `00-indice-arquitectura.md`).
- Los errores siempre usan `application/problem+json` (RFC 7807) con el schema `ProblemDetails`, que añade un campo `code` con los códigos de negocio ya definidos en `02-arquitectura.md`.

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

`409` se reserva para los casos donde el frontend debe releer el estado (calendario desactualizado); `422` para violaciones de regla que no dependen de una carrera de concurrencia; `403` para falta de autorización/relación. Este criterio es el mismo que ya recomendaba `04-frontend-engineer-next-react.md` para tratar las respuestas `409` como "el calendario puede estar obsoleto, vuelve a consultarlo".

## Endpoints cubiertos

El spec cubre los 24 endpoints ya listados en `02-arquitectura.md` §8, agrupados en los mismos cuatro bloques: `auth`/`me`, `teacher`/`students`, `availability`, `lesson`/`booking`/`calendar`, `administration`. Ver `openapi.yaml` para el detalle de schemas, parámetros y respuestas de error de cada uno.

## Pendiente al pasar a implementación

- Generar el cliente TypeScript tipado del frontend a partir de este spec (p. ej. `openapi-typescript`), en vez de escribir los tipos a mano, para que un cambio de contrato rompa la build en vez de fallar en runtime.
- Añadir ejemplos (`examples:`) por endpoint una vez haya payloads reales de referencia acordados con QA.
- Revisar si `PATCH /me` necesita separarse en endpoints específicos por rol (`/teacher/profile` y perfil de alumno ya son independientes) para evitar un DTO demasiado genérico; se deja como está por ahora porque el propio `02-arquitectura.md` ya lo definía así.
