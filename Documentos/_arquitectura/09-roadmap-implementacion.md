# Tennis Platform — Roadmap de implementación del MVP

## Objetivo

Definir un orden de construcción concreto para el MVP, respetando el grafo de dependencias entre módulos descrito en `02-arquitectura.md` (`identity → teacher → student → availability → lesson → booking → administration/calendar`). Cada fase debe dejar el sistema en un estado desplegable y probado antes de avanzar a la siguiente; no se debe empezar un módulo cuyas dependencias no estén ya operativas.

## Fase 0 — Scaffold del repositorio

- Monorepo con `frontend/`, `backend/`, `docs/`, `docker/`, `.github/workflows/`.
- `docker-compose` con PostgreSQL local.
- Esqueleto Spring Boot con el módulo `shared` (IDs, errores comunes, reloj) y la estructura hexagonal por módulo ya creada (carpetas vacías `domain/`, `application/port/in|out`, `application/service`, `adapters/in/web`, `adapters/out/persistence`, `configuration`).
- Esqueleto Next.js con Tailwind y el cliente HTTP centralizado (sin endpoints reales todavía).
- Primer changelog de Liquibase (vacío, solo estructura) y pipeline de CI mínima (build backend, build frontend).

Criterio de salida: `docker-compose up` levanta backend, frontend y base de datos, y la CI pasa en verde sin funcionalidad de negocio.

## Fase 1 — `identity`

- Registro público, verificación de email (usa el SMTP genérico decidido), login, logout, refresh (cookie `HttpOnly`), recuperación de contraseña.
- Rate limiting básico en los endpoints de autenticación.
- Frontend: páginas de registro, verificación, login, recuperación/reseteo.
- Tests: unitarios de dominio, tests de aplicación con puertos simulados, tests REST, E2E de registro→verificación→login.

Criterio de salida: un usuario puede registrarse, verificar su email y autenticarse de principio a fin, en frontend y backend reales.

## Fase 2 — `teacher` y `student`

- Bootstrap del profesor único (seed/migración con credenciales iniciales).
- Perfil de profesor y alumno (nombre, teléfono, DNI, dirección).
- Asociación alumno-profesor por búsqueda de email (`teacher_students`).
- Frontend: perfil propio, listado y gestión de alumnos por el profesor.
- Tests: gestión de alumno, acceso horizontal (un alumno no ve datos de otro).

Criterio de salida: el profesor puede buscar un alumno registrado por email, gestionarlo, y el alumno ve reflejado su estado de "gestionado".

## Fase 3 — `availability`

- Reglas semanales de disponibilidad, excepciones por fecha, bloqueos.
- Frontend: pantalla de configuración de disponibilidad semanal y excepciones.
- Tests: solapamientos de reglas, excepciones, cambios de horario de verano/invierno.

Criterio de salida: el profesor puede definir su disponibilidad semanal y excepciones, consultable por API.

## Fase 4 — `lesson`

- Creación de clases individuales y grupales, validación de duración (múltiplo de 30, no cruza medianoche), validación contra disponibilidad (con override explícito del profesor), listado por rango y cancelación.
- Estados `OPEN`, `FULL`, `CANCELLED`, `COMPLETED`, de los cuales **sólo los dos primeros llegan a esta fase y sólo dos se almacenan**: `FULL` necesita contar reservas, que es cosa de `booking`. Ver `19-fase8-analisis-lesson.md`.
- **La modificación de una clase queda fuera**, aunque este documento la prometía aquí. Cambiar la hora de una clase que ya tiene reservas es una operación que afecta a `booking`, y escribirla antes de que `booking` exista significa escribirla dos veces. Cancelar y volver a crear cubre el caso mientras tanto.
- Frontend: creación de clase, detalle de clase.
- Tests: reglas de duración, solapamiento de clases del profesor, override de disponibilidad.

Criterio de salida: el profesor puede crear clases individuales y grupales respetando (o forzando explícitamente) su disponibilidad.

## Fase 5 — `booking` (ruta crítica de concurrencia)

- Reserva automática con transacción completa: bloqueo de la clase, comprobación de estado/capacidad, duplicados, solapamiento del alumno.
- Cancelación con ventana de 24 horas (y excepción ya resuelta para `ADMIN`).
- Marcado de asistencia.
- Frontend: reservar, ver mis reservas, cancelar, próximas clases.
- Tests: concurrencia de última plaza (obligatorio antes de cerrar esta fase), duplicados, solapamientos, cancelación dentro/fuera de ventana.

Criterio de salida: dos usuarios pueden intentar reservar la última plaza simultáneamente y el sistema garantiza que solo uno gana, sin excepción no controlada.

Cumplido en la Fase 9 de `12-metodologia-trabajo.md` por `LastSeatConcurrencyTest`, que además
falla si se quita el cerrojo. La fase cerró también lo que la Fase 8 le dejó —`bookedCount`,
`FULL`, la cascada al cancelar una clase— y la deuda de la Fase 6: desactivar un alumno cancela
sus reservas futuras. Ver `20-fase9-analisis-booking.md`.

## Fase 6 — `calendar`

- Vista agregada de disponibilidad, clases y reservas, en hora local del usuario con la zona horaria del profesor visible.
- Frontend: calendario completo integrando los tres tipos de datos.
- Tests: que `calendar` no escriba en dominios ajenos (ArchUnit), consistencia de zonas horarias.

Criterio de salida: el calendario del alumno y del profesor reflejan el estado real del backend sin duplicar lógica de negocio.

## Fase 7 — `administration`

- Consola de configuración global (límite de alumnos), gestión de usuarios y su estado.
- Frontend: pantallas de administración.
- Tests: solo `ADMIN` accede; cambios de límite y de estado de usuario.

Criterio de salida: el `ADMIN` puede ajustar el límite de alumnos y gestionar el estado de cualquier usuario.

## Fase 8 — Endurecimiento previo al lanzamiento

- PWA completa (manifest, service worker, shell offline).
- Accesibilidad (navegación por teclado, contraste, labels).
- Revisión de seguridad previa al lanzamiento (checklist completo de `08-security-engineer.md`).
- Observabilidad (Actuator, correlation ID, métricas de reservas/conflictos/cancelaciones).
- Decisión real de proveedor de producción (aplazada hasta esta fase, ver `06-devops-engineer.md`) y política de backups.

Criterio de salida: checklist de seguridad y observabilidad completado; el sistema está listo para exponerse con usuarios reales, salvo el riesgo aceptado de backups que debe resolverse antes de producción real con datos reales.

## Notas sobre paralelismo

- El frontend de una fase puede empezar en cuanto el contrato de API de esa fase esté estable, sin esperar a que el backend esté 100% terminado, siempre que se trabaje contra un contrato (ver punto pendiente del contrato OpenAPI).
- `administration` (fase 7) es funcionalmente independiente de `lesson`/`booking` y podría adelantarse en paralelo a las fases 4-6 si hay dos personas trabajando a la vez; se deja al final en este roadmap solo por simplicidad de secuenciación con una sola persona/equipo.
- Ninguna fase de negocio (2 en adelante) debe empezar antes de que `identity` esté completo, porque todas dependen de autenticación y roles.
