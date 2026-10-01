# Fase 19 — Análisis: cierre del MVP

Criterio de salida (`12-metodologia-trabajo.md`):
- cambiar la contraseña, exportar la cuenta y borrarla;
- el admin lista reservas y cancela clases;
- editar las notas y la capacidad de una clase.

El borrado anonimiza (`01` §18).

Son cinco piezas pequeñas y sueltas. Van en un solo PR, con un commit por pieza.

## Punto de partida

- **Contraseña.** Sólo se cambia con "olvidé mi contraseña". `ResetPasswordService` ya hace lo
  necesario: aplica la política, cambia el hash y revoca todas las sesiones.
- **Datos personales.**
  - En `users`: el email.
  - En `student_profiles`: nombre, teléfono, DNI y dirección.
  - Las notas de una clase las escribe el profesor y pueden nombrar a alguien.
  - Los tokens sólo guardan hashes.
- **Exportar y borrar**: no existen. `/me` sólo tiene GET y PATCH.
- **Desactivar a un alumno** ya libera sus plazas en una sola transacción: `ManageStudent`
  llama a `StudentBookings`, que implementa `booking`. El borrado sigue ese camino.
- **El admin**:
  - ya puede cancelar cualquier reserva (`POST /bookings/{id}/cancel`, `CANCELLED_BY_ADMIN`);
  - no ve ni el calendario ni las reservas: `/calendar` y `GET /bookings` le dan 403;
  - no puede cancelar clases.
- **Una clase no se puede tocar** después de crearla. La tabla ya tiene `notes`, `capacity` y
  `version`.
- **Reservar bloquea la clase** con `SELECT ... FOR UPDATE` (`LockLesson`).

## Decisiones propuestas

### 1. Cambiar la contraseña: `POST /me/password`

- **Pide la contraseña actual y la nueva.** Así, un access token robado no basta para quedarse
  con la cuenta.
- **Cierra las demás sesiones y abre una nueva** en este dispositivo. Responde como el login:
  access token en el cuerpo y cookie de refresco nueva. Quien cambia la contraseña porque
  sospecha algo echa a los demás sin echarse a sí mismo.
- **Vale para los tres roles.**
- **Contraseña actual incorrecta**: 422 `CURRENT_PASSWORD_INCORRECT`. Releer no lo cambia.
- **La nueva tiene que pasar `PasswordPolicy`**, como en el registro.
- **Límite de peticiones.** Entra en el rate limiting por IP que ya cubre `/auth/*`. Si no,
  sería un sitio donde probar contraseñas sin límite.

### 2. Exportar mis datos: `GET /me/export`

Un JSON que se descarga (`Content-Disposition: attachment`) con:
- la cuenta: email, rol, alta y verificación;
- el perfil;
- para el alumno, todas sus reservas con la hora de la clase, su estado y la asistencia.

Lo compone `web`, como ya hace `/me`, sólo con puertos de entrada. A `GetBookings` se le añade
una lectura completa del alumno, sin paginar. Vale para los tres roles; el profesor y el admin
reciben la cuenta y el perfil.

### 3. Borrar mi cuenta: `POST /me/deletion`

- **Sólo el alumno.** Al profesor lo crea el bootstrap y al admin también: ninguno se borra
  desde la app (403).
- **Pide la contraseña.** Es irreversible, y un token robado no debería bastar.
- **Es un `POST` y no `DELETE /me`**: lleva cuerpo, y la cuenta no desaparece, se anonimiza.
- **Pasa todo en una transacción**, coordinada por un servicio de `student`: el módulo que ya
  puede hablar con `identity` y, por su SPI, con `booking`.
  - Sus **reservas futuras se cancelan** como `CANCELLED_BY_STUDENT`, porque lo decidió él. Las
    de clases ya empezadas se quedan, como al desactivarlo.
  - La **gestión** con el profesor pasa a inactiva.
  - El **perfil** queda con `full_name = 'Alumno eliminado'` y teléfono, DNI y dirección a
    `null`.
  - La **cuenta**:
    - el email pasa a `deleted-<uuid>@invalid`, único y sin dueño, y el real queda libre para
      volver a registrarse;
    - el hash de contraseña pasa a uno imposible;
    - el estado pasa a uno nuevo, **`DELETED`**;
    - se revocan sus sesiones y se borran sus enlaces pendientes.
- **`DELETED` no es `DISABLED`.** No inicia sesión y **el admin no puede reactivarla**: no hay
  nadie a quien devolverle la cuenta. En la consola sale como "Eliminada". Hace falta un
  changeset nuevo para ampliar el `CHECK` de `users.status`.
- **Las notas de las clases no se tocan.** Son texto libre del profesor. Se anota en `01` que
  no deben llevar datos de alumnos.

### 4. Incidencias del admin

- **`GET /calendar`** responde también al admin, con la vista del profesor: toda la agenda, con
  el recuento de plazas. `calendar` sigue siendo de sólo lectura.
- **`GET /bookings`** responde también al admin, con todas las reservas y los filtros
  `lessonId` y `status`.
- **`POST /admin/lessons/{id}/cancel`**, nuevo, en el adaptador web de `lesson`:
  - hace lo mismo que la cancelación del profesor, sin ventana, y sus reservas pasan a
    `CANCELLED_BY_ADMIN`, para que el alumno sepa quién lo decidió;
  - `CancelLesson` gana `asAdmin`, y `LessonBookings.cancelAllOf` recibe quién cancela;
  - no reutiliza `/teacher/lessons/{id}/cancel`: el prefijo dice quién puede llamarlo.
- **Pantallas del admin:**
  - "Clases": la semana, con el mismo calendario del profesor en modo lectura;
  - el detalle de una clase, con sus reservas, "Cancelar clase" y "Cancelar reserva".
  - El alumno se muestra por su email, que el admin ya ve en Usuarios. No se añade el nombre a
    `BookingView`.

### 5. Editar una clase: `PATCH /teacher/lessons/{id}`

- **Sólo las notas y la capacidad.** La hora sigue sin cambiar (Fase 8).
- **Sólo antes de que empiece**, y no si está cancelada: los 409 y 422 que ya existen
  (`LESSON_ALREADY_CANCELLED`, `LESSON_ALREADY_STARTED`).
- **La capacidad de una clase individual sigue siendo 1.** La de una grupal, entre 2 y
  `max_group_capacity`: la misma regla que al crearla, en el dominio (`Lesson.edit`).
- **No puede bajar por debajo de las reservas confirmadas**: 409
  `LESSON_CAPACITY_BELOW_BOOKINGS`. Es un 409 porque, si alguien cancela, releer cambia la
  respuesta.
- **Bloquea la clase** como una reserva (`FOR UPDATE`) antes de contar. Sin eso, una reserva y
  una bajada de capacidad simultáneas podrían dejar más alumnos que plazas.
- En el frontend, el detalle de la clase del profesor gana "Editar".

## Contrato

Todo va a `openapi.yaml` primero:
- `npm run api:types`;
- una fila por operación en `AccessMatrixTest`;
- los códigos nuevos en `11-contrato-api.md`: `CURRENT_PASSWORD_INCORRECT` y
  `LESSON_CAPACITY_BELOW_BOOKINGS`.

`UserStatus` gana `DELETED`, y `BookingView` no cambia.

## Frontend

- **Perfil**, en tres bloques:
  - cambiar la contraseña;
  - descargar mis datos;
  - borrar la cuenta, sólo el alumno, con un diálogo que explica qué se borra y pide la
    contraseña. Después, a una pantalla de despedida sin sesión.
- **Profesor**: "Editar" en el detalle de la clase, con las notas y la capacidad.
- **Admin**: "Clases" en la navegación, con la semana y el detalle de la clase.

Skills:
- **frontend-design**: el diálogo de borrado es la única acción destructiva sin vuelta atrás, y
  tiene que parecerlo, sin alarmismo.
- **react-best-practices**: el calendario y el detalle se reutilizan, no se copian.
- **playwright-cli**: cada pantalla a 390 y a 1440 px.

## Fuera, y por qué

- **Cambiar el email**: sigue fuera (01 §4). Exige verificar la dirección nueva.
- **Borrar por petición al admin**: el alumno puede hacerlo solo.
- **Cambiar la hora de una clase con reservas**: se cancela y se crea otra.
- **Avisar por correo** de una cancelación del admin: Fase 20.
- **Ver quién canceló una clase**: las reservas ya lo dicen. La clase no gana columna.

## Tests

- **Contraseña**:
  - con la actual correcta, el refresh de otra sesión da 401 y el de ésta funciona;
  - con la incorrecta, 422 y nada cambia;
  - la nueva débil, 400.
- **Exportar**: el JSON trae las reservas del alumno y nada de otro alumno.
- **Borrar**:
  - con la contraseña, el login falla y el email vuelve a poder registrarse;
  - la reserva futura queda cancelada y la plaza libre; la pasada sigue, con "Alumno eliminado";
  - el admin no puede reactivarla;
  - el profesor y el admin reciben 403.
  - **Sin rastro**: un test recorre todas las columnas de texto de la base y no encuentra ni el
    email, ni el nombre, ni el teléfono, ni el DNI del alumno borrado.
- **Admin**:
  - ve la agenda y las reservas;
  - cancela una clase, y sus reservas quedan `CANCELLED_BY_ADMIN`;
  - el profesor recibe 403 en la ruta del admin.
- **Editar**:
  - la capacidad baja hasta las reservas, pero no por debajo (409);
  - la clase individual no cambia;
  - la clase empezada no se edita.
  - **Carrera**: dos alumnos reservan mientras el profesor baja la capacidad a 1, y nunca quedan
    más confirmadas que plazas, como `LastSeatConcurrencyTest`.
- **E2E**:
  - el alumno cambia la contraseña y entra con la nueva;
  - el alumno se borra y el profesor lo ve como "Alumno eliminado";
  - el admin cancela una clase y el alumno la ve cancelada;
  - el profesor edita la capacidad.

**Mutaciones**, cada una se ve fallar y se deshace:
- no revocar las otras sesiones;
- dejar el teléfono sin borrar;
- quitar el cerrojo al editar;
- dejar pasar al profesor por la ruta del admin.

## Criterios de aceptación

- Existe un test que demuestra que, tras borrar la cuenta, ningún dato personal del alumno queda
  en la base.
- Existe un test que demuestra que cambiar la contraseña cierra las demás sesiones y conserva
  ésta.
- Existe un test que demuestra que editar la capacidad no deja más reservas que plazas, en
  carrera.
- Existe un test que demuestra que la cancelación del admin queda como `CANCELLED_BY_ADMIN`.
- Las cuatro mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y los E2E nuevos pasan.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Contraseña.
3. Exportar.
4. Borrar.
5. Admin.
6. Editar.
7. Frontend, pieza a pieza.
8. E2E y documentación: `01` §4, la trampa de `DELETED` y `TennisPlatformApp/CLAUDE.md`.

## Decisiones tomadas al implementar

- **`DELETED` es definitivo también en el dominio.** Ni desactivar ni reactivar (422
  `ACCOUNT_DELETED`), y un enlace de verificación o de restablecimiento pendiente ya no la
  devuelve: `User` sólo admite esas transiciones desde `ACTIVE` o `PENDING_VERIFICATION`.
- **El profesor ve a un alumno borrado como «Alumno».** Su lista sólo trae a los alumnos que
  gestiona, así que un alumno borrado sale como cualquiera al que dejó de gestionar. En la base
  queda «Alumno eliminado». Enseñar el nombre de los antiguos alumnos encaja en la ficha de la
  Fase 23.
- **Borrar la cuenta también cuenta para el límite por IP**, como cambiar la contraseña: las dos
  comprueban una contraseña con un access token.
- **El admin ve las notas de una clase igual que hoy: sin ellas.** La respuesta de su cancelación
  y `GET /lessons/{id}` las reservan al profesor.
- **Dos tests fijaban la regla anterior** («el admin no tiene calendario» y «no tiene lista de
  reservas»). Se han quitado; `AdminIncidentsTest` prueba la nueva.
- **La semana de la agenda pasa a `AgendaWeek`**, en `calendar`, porque la usan el profesor y el
  admin. Antes estaba entera en la página del profesor.
- **Tras borrar la cuenta, el login lo confirma** (`/login?cuenta=borrada`). Era el único sitio
  sin sesión al que llevar al alumno sin crear una página pública nueva.
- **Los formularios con contraseña llevan el usuario oculto** (`autocomplete="username"`), para que
  el gestor de contraseñas sepa de qué cuenta es. Lo pedía Chrome en la consola.

