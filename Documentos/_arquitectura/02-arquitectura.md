# Tennis Platform — Arquitectura técnica

## 1. Arquitectura general

La solución inicial será:

Frontend Next.js/React/TypeScript/PWA
→ REST API
→ Java 21/Spring Boot
→ PostgreSQL

El backend será un Modular Monolith con arquitectura hexagonal en cada módulo.

## 2. Por qué Modular Monolith

No se necesitan microservicios porque:

- El producto tiene un único dominio principal.
- Las reservas necesitan consistencia transaccional.
- No hay necesidades de escalado independiente.
- El equipo se beneficia de un despliegue único.
- Kafka, service discovery y tracing distribuido introducirían complejidad innecesaria.

Los límites modulares permitirán extraer un módulo en el futuro si aparece una razón técnica real.

## 3. Módulos backend

### identity

Registro, login, logout, refresh, verificación de email, recuperación de contraseña, roles, sesiones y ciclo de vida de usuarios.

### teacher

Perfil, estado y zona horaria del profesor único.

### student

Perfil del alumno y relación de alumno gestionado por el profesor. La gestión se establece cuando el profesor busca al alumno por email y lo asocia explícitamente.

### availability

Reglas semanales, excepciones, bloqueos y evaluación de disponibilidad.

### lesson

Clases individuales y grupales, horarios, capacidad, estados y asistencia.

### booking

Reservas, capacidad, duplicados, solapamientos y cancelaciones.

### administration

Consola administrativa y usuarios. **Edita** la configuración global a través del módulo
`platform`, pero no es su dueño: ver más abajo.

### platform

Configuración global de la instalación: hoy, el límite de alumnos gestionados
(`platform_configuration`).

Este módulo no estaba en la versión original de este documento, que asignaba
`platform_configuration` a `administration`. **Era un error de propiedad**, no un problema de
ciclos: `student` necesita leer el límite para validarlo al asociar un alumno, y `student` no
puede depender de `administration`. El dueño de una tabla es el módulo dueño del dato, no el
que tiene la pantalla más bonita para editarlo.

Se corrige creando `platform`, que no depende de nadie y puede por tanto ser leído por todos.
En la Fase 6 se implementa **solo el lado de lectura** (la tabla, el puerto `GetStudentLimit`,
su servicio y su adaptador); la consola que cambia el valor sigue siendo la Fase 7, y vivirá en
`administration` llamando a un puerto de escritura de `platform`.

`platform_configuration.updated_by` se mantiene como clave ajena a `users`. En código
`platform` no depende de `identity` —solo guarda un UUID—, pero en el esquema la referencia
existe: la integridad referencial de un campo de auditoría vale más que la pureza del diagrama.

### calendar

Consulta agregada de disponibilidad, clases y reservas. No es propietario de los datos.

### shared

Primitivas técnicas mínimas: identificadores, errores comunes, reloj e infraestructura compartida. No contiene lógica de negocio específica.

## 4. Dependencias permitidas

- `identity` no depende de módulos de negocio.
- `teacher` puede consultar identity.
- `student` puede consultar identity, teacher y platform.
- `availability` puede consultar teacher.
- `lesson` puede consultar teacher y availability.
- `booking` puede consultar student, lesson e identity (esta última por el adaptador web, igual
  que el resto de módulos con endpoints). Además **implementa** las interfaces que `lesson` y
  `student` declaran en su paquete `application/port/spi`: así cancelar una clase o desactivar a
  un alumno cancela sus reservas, y leer una clase cuenta sus plazas, sin que ninguno de los dos
  dependa de `booking` al compilar (Fase 9, `20-fase9-analisis-booking.md`).
- `administration` puede consultar identity, teacher, student y platform.
- `platform` no depende de ningún módulo: es la configuración global y la lee todo el mundo.
- `calendar` solo usa interfaces públicas de consulta.

No se permiten accesos directos a repositorios, entidades JPA o adaptadores internos de otro módulo.

## 5. Arquitectura hexagonal

Cada módulo tendrá:

- `domain`: entidades, objetos de valor, reglas y excepciones.
- `application`: casos de uso y puertos.
- `adapters/in`: REST controllers y adaptadores de entrada.
- `adapters/out`: persistencia y servicios externos.
- `configuration`: ensamblado de dependencias.

El dominio no dependerá de Spring, JPA, REST ni PostgreSQL.

## 6. Estructura del proyecto

backend/

- `identity/`
- `teacher/`
- `student/`
- `availability/`
- `lesson/`
- `booking/`
- `administration/`
- `calendar/`
- `platform/`
- `shared/`

Dentro de cada módulo:

- `domain/`
- `application/port/in/`
- `application/port/out/`
- `application/service/`
- `adapters/in/web/`
- `adapters/out/persistence/`
- `configuration/`

La organización por módulo es preferible a una estructura global de controllers/services/repositories porque mantiene juntas las reglas de cada contexto.

## 7. Modelo de aplicación

Los controllers solo traducen HTTP a comandos y respuestas.

Los casos de uso principales son:

- `RegisterStudent`.
- `VerifyEmail`.
- `LoginUser`.
- `ResetPassword`.
- `ManageStudent`.
- `ConfigureWeeklyAvailability`.
- `CreateLesson`.
- `BookLesson`.
- `CancelBooking`.
- `CancelLesson`.
- `MarkAttendance`.
- `UpdateStudentLimit`.

## 8. REST API inicial

### Identity

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/verify-email`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `GET /api/v1/me`
- `PATCH /api/v1/me`

### Teacher y students

- `GET /api/v1/teacher/profile`
- `PATCH /api/v1/teacher/profile`
- `GET /api/v1/teacher/students`
- `GET /api/v1/teacher/students/lookup`
- `GET /api/v1/teacher/students/{userId}`
- `POST /api/v1/teacher/students/{userId}/manage`
- `DELETE /api/v1/teacher/students/{userId}/manage`

Las dos rutas nuevas aparecen en la Fase 6 y no estaban en la lista original. No son alcance
añadido: son lo que hacen falta para cumplir los criterios de aceptación acordados en
`16-fase6-analisis-perfiles.md`.

- `GET /teacher/students/lookup?email=` busca por **email exacto y completo** al alumno que se
  va a asociar. Antes ese trabajo se lo repartía el parámetro `?query=` de la lista, que servía
  a la vez para filtrar entre los alumnos propios y para localizar a cualquiera del sistema.
  Con búsqueda parcial sobre todas las cuentas, un profesor podía **enumerar quién está
  registrado** y leer nombres de personas con las que no tiene ninguna relación, que es
  justamente lo que prohíbe `02-arquitectura.md`. Separadas, `?query=` solo filtra entre
  los alumnos ya gestionados, donde la coincidencia parcial no expone nada nuevo.
- `GET /teacher/students/{userId}` devuelve la ficha completa —con `national_id` y `address`—
  de **un alumno que ese profesor gestiona**. Sin ella no había forma de cumplir los criterios
  1 y 2: no existía ningún endpoint que devolviese datos restringidos y, por tanto, ninguno al
  que exigirle que los negara a quien no tiene relación con el alumno.

### Availability

- `GET /api/v1/teacher/availability`
- `PUT /api/v1/teacher/availability/weekly`
- `POST /api/v1/teacher/availability/exceptions`
- `DELETE /api/v1/teacher/availability/exceptions/{id}`

### Lessons y bookings

- `GET /api/v1/calendar`
- `POST /api/v1/teacher/lessons`
- `GET /api/v1/lessons/{id}`
- `POST /api/v1/lessons/{id}/bookings`
- `GET /api/v1/bookings`
- `POST /api/v1/bookings/{id}/cancel`
- `POST /api/v1/teacher/lessons/{id}/cancel`
- `POST /api/v1/teacher/lessons/{id}/attendance`

### Administration

- `GET /api/v1/admin/configuration`
- `PATCH /api/v1/admin/configuration/student-limit`
- `GET /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{id}/status`

## 9. DTOs

Los DTOs estarán separados del dominio.

Principales grupos:

- Authentication: registro, login, verificación, recuperación y sesión.
- Profile: perfil común y perfil de profesor/alumno.
- Availability: reglas y excepciones.
- Lesson: creación, modificación, consulta y asistencia.
- Booking: creación, consulta y cancelación.
- Administration: configuración y usuarios.

No se expondrán entidades JPA, contraseñas, tokens ni datos privados innecesarios.

## 10. Persistencia

Los repositorios serán puertos:

- `UserRepository`.
- `TeacherProfileRepository`.
- `StudentProfileRepository`.
- `TeacherStudentAssociationRepository`.
- `AvailabilityRuleRepository`.
- `AvailabilityExceptionRepository`.
- `LessonRepository`.
- `BookingRepository`.
- `PlatformConfigurationRepository`.

Las implementaciones JPA estarán en `adapters/out/persistence` de cada módulo.

## 11. Transacciones

Los límites transaccionales estarán en la capa de aplicación.

La reserva debe:

1. Bloquear la clase.
2. Comprobar estado y capacidad.
3. Comprobar duplicado.
4. Bloquear o serializar la comprobación del alumno.
5. Comprobar solapamientos.
6. Crear la reserva.

La base de datos y la aplicación deben proteger conjuntamente la consistencia.

## 12. Errores

Se utilizará Problem Details con códigos como:

- `LESSON_FULL`.
- `LESSON_OVERLAP`.
- `BOOKING_ALREADY_EXISTS`.
- `STUDENT_NOT_MANAGED`.
- `CANCELLATION_WINDOW_EXPIRED`.
- `STUDENT_SCHEDULE_OVERLAP`.

Mapeo general:

- 400: formato o validación.
- 401: no autenticado.
- 403: sin permisos.
- 404: no encontrado.
- 409: conflicto.
- 422: regla de negocio.
- 500: error inesperado.

## 13. Seguridad

- Access token corto en `Authorization: Bearer`; refresh token rotatorio en cookie `HttpOnly`,
  `Secure`, `SameSite`, guardado como hash y con familia para detectar reutilización. Nunca en
  `localStorage` ni en el cuerpo de una respuesta.
- CSRF sólo en `/auth/refresh` y `/auth/logout`, los dos endpoints que autentica la cookie.
- Contraseñas con BCrypt. Tokens de recuperación de un solo uso y con caducidad.
- **Autorización por rol y por propiedad**, siempre las dos. Ocultar un botón no es autorización.
  Cambiar un id en la URL no debe dar acceso a nada ajeno: lo ajeno responde 404.
- Nunca confiar en roles ni en ids de propiedad que vengan en el cuerpo.
- Sin enumeración: registro, recuperación y búsqueda no revelan si una cuenta existe.
- Rate limiting en autenticación.
- Nunca registrar contraseñas, tokens, DNI ni dirección completos.
- `ADMIN` no se registra públicamente.
- La PWA sólo cachea el shell y datos no personales; nada de reservar ni cancelar sin conexión.

## 14. Observabilidad

- Spring Actuator.
- Health checks.
- Logs estructurados.
- Correlation ID.
- Métricas HTTP.
- Métricas de reservas, conflictos y cancelaciones.

## 15. Testing

- Unit tests de dominio sin Spring.
- Tests de aplicación con puertos simulados.
- Tests REST.
- Tests con PostgreSQL real mediante Testcontainers.
- Tests de concurrencia.
- ArchUnit para dependencias.
- Tests E2E con Playwright.

## 16. Configuración

Perfiles:

- `local`.
- `test`.
- `staging`.
- `prod`.

Variables sensibles fuera de Git:

- Base de datos.
- Secretos JWT.
- CORS.
- Email (host, puerto, credenciales SMTP).
- Parámetros de negocio.

## 17. Frontend

Next.js con TypeScript estricto y Tailwind, organizado por funcionalidad (`authentication`,
`profile`, `students`, `availability`, `calendar`, `lessons`, `bookings`, `administration`).

- **Un único cliente HTTP** añade credenciales, renueva la sesión, normaliza los errores Problem
  Details y tipa las respuestas. Ningún componente llama a la API por su cuenta.
- El backend es la fuente de verdad de clases, reservas y disponibilidad. Un `409` significa
  "lo que tienes en pantalla está obsoleto": se relee y se vuelve a mostrar.
- Las fechas se muestran en la hora local del usuario, indicando la zona cuando pueda haber
  ambigüedad.
- La validación en el cliente es comodidad; la que vale es la del backend.
- Accesibilidad básica: teclado, etiquetas, errores asociados a su campo, confirmación de
  acciones destructivas.

## 18. ADRs principales

- Modular Monolith.
- Arquitectura hexagonal.
- Módulos por capacidad de negocio.
- `identity` como contexto único para auth y usuarios.
- PostgreSQL y Liquibase.
- UTC e IANA timezones.
- Reservas automáticas.
- Control transaccional de concurrencia.
- Disponibilidad como restricción con override del profesor.
- Calendar como consulta agregada.
- Sin microservicios ni Kafka en el MVP.

## Cómo se hacen cumplir estos límites (18/09/2026)

Los límites de este documento dejan de ser una convención y pasan a estar verificados por 21
reglas de ArchUnit en `ModuleBoundariesTest`, que corren en cada `mvn test`. Cuatro decisiones
los hacen comprobables; el razonamiento completo está en
`17-analisis-archunit-limites-modulares.md`:

1. **`config`, `error` y `web` no son módulos**: son núcleo técnico exento, fuera del grafo.
   Cualquier módulo puede usarlos, y el *composition root* puede ver cualquier módulo porque
   ensamblar implementaciones concretas es su función. No se absorben en `shared`, que queda
   reservado a primitivas reutilizables.
2. **Las reglas cubren los nueve módulos**, también los que aún están vacíos: el grafo ya está
   decidido y así cada módulo nace vigilado.
3. **`calendar` solo puede usar puertos `Get`, `Find` o `Query`.** Esto convierte "solo
   interfaces públicas de consulta" en algo mecánico, y fija la convención de nombres de los
   puertos de todos los módulos.
4. **Un módulo solo puede importar de otro lo que cuelgue de `application/port/in`.** Su
   dominio, sus servicios, sus puertos de salida, sus adaptadores y su configuración son su
   interior.

La excepción declarada es `@Transactional` en `application/service`, porque este documento sitúa
ahí las fronteras transaccionales. Está escrita en el propio test con su motivo al lado, no como
una exclusión muda.
