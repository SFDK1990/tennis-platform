# Fase 20 — Análisis: avisos y calendario

Criterio de salida (`12-metodologia-trabajo.md`):
- correos de reserva y cancelación al alumno, y aviso a Marcos;
- `.ics`;
- cancelar un día entero por lluvia.

Las reglas ya están decididas (`01` §18): al alumno, la reserva confirmada (con `.ics`) y la
cancelación de su clase o de su reserva por el profesor o el admin; a Marcos, cada reserva y
cada cancelación de un alumno. Sin recordatorio previo.

## Punto de partida

- **Sólo `identity` envía correos** (`SmtpIdentityMailer`): texto plano, en la misma petición, y
  un fallo del SMTP se registra sin la dirección y no deshace nada (Fase 16).
- **Los correos dicen "Tennis Platform"**; la app ya es la Escuela de Tenis Oasis Papagayo
  (Fase 31).
- **Reservar y cancelar** viven en `booking`: `BookLessonService`, `CancelBookingService` y,
  como implementación de los puertos de `lesson` y `student`, `BookingsOfLessons` y
  `BookingsOfStudents`. Estos dos últimos cancelan en bloque con un `UPDATE`, sin cargar las
  reservas.
- **El email de un usuario** se pide a `identity` con `FindUserAccounts.byIds`; la zona horaria
  del profesor, a `teacher`.
- **No hay eventos ni nada asíncrono** en el backend.
- **Una clase se cancela de una en una** (`POST /teacher/lessons/{id}/cancel`).
- **En la Pi no hay correo configurado** (`MAIL_*` pendiente, Fase 17.1): hasta entonces estos
  correos fallan allí sin romper nada, como los de verificación.

## Decisiones propuestas

### 1. Un módulo `notification` que escucha, no un servicio al que se llama

- `booking` y `lesson` **publican hechos** (`BookingConfirmed`, `BookingCancelled`) al terminar
  cada caso de uso. No saben quién los escucha ni que existe el correo.
- `notification` los recibe **después del commit** (`@TransactionalEventListener(AFTER_COMMIT)`):
  - una reserva que se deshace (la última plaza perdida en una carrera) no manda un correo
    falso;
  - un SMTP caído no deshace la reserva.
- **Se envían en segundo plano**, con un ejecutor pequeño y acotado. Cancelar un día de lluvia
  con ocho clases son decenas de correos, y la respuesta no puede esperar a cada uno.
- `notification` puede consultar `booking`, `lesson`, `teacher` e `identity`. Nadie depende de
  él. Se añade a `02-arquitectura.md` §4 y a `ModuleBoundariesTest`.
- Los hechos llevan ids y el estado, no direcciones ni nombres: quien envía los busca.

**La alternativa descartada**, que `booking` llame a un puerto `Notifier`, envía dentro de la
transacción: un correo de "reserva confirmada" para una reserva que luego se deshace.

**Lo que se acepta perder** (aceptado por Daniel): si el servidor se cae entre el commit y el envío,
ese correo no sale. Evitarlo pide un *outbox* (una tabla de correos pendientes y reintentos).
Con un profesor, una pista y sin recordatorios, no se hace ahora: la reserva queda bien hecha y
el alumno la ve en la app.

### 2. Qué correo sale y a quién

| Hecho | Alumno | Marcos |
|---|---|---|
| Reserva confirmada | Sí, con `.ics` | Sí |
| El alumno cancela | No: lo acaba de hacer él | Sí |
| El profesor cancela la reserva o la clase | Sí, con `.ics` de cancelación | No: lo hizo él |
| El admin cancela la reserva o la clase | Sí, con `.ics` de cancelación | No |
| El profesor deja de gestionar al alumno, o el admin lo desactiva | Sí, una por reserva cancelada | No |
| El alumno borra su cuenta | No: ya no hay a quién | No |

- **A Marcos, sólo lo que hacen los alumnos**, como dice `01` §18: así cada correo depende sólo
  de quién canceló.
- **Un correo por reserva**, no un resumen: cada uno lleva su `.ics` y su hora.
- **Texto plano, en español**, con la fecha y la hora de pared del profesor ("martes 14 de
  octubre, de 10:00 a 11:00"), como las ve el alumno en la app.
- **Firmados por la escuela.** Los correos de `identity` pasan también a decir Oasis Papagayo.

### 3. El `.ics`

- **Adjunto** al correo, `text/calendar; method=REQUEST` al reservar y `method=CANCEL` al
  cancelar.
- **El mismo `UID` en los dos** (el id de la reserva): el calendario del alumno quita el evento
  en vez de dejarlo huérfano.
- Horas en UTC (`DTSTART:20261014T080000Z`): ningún calendario se equivoca de zona.
- **Escrito a mano**, sin librería: son una docena de líneas. Lo que falla en silencio son el
  escapado de comas y saltos de línea, el plegado a 75 octetos y el `CRLF`, y eso lo cubren los
  tests.

### 4. Cancelar un día entero: `POST /teacher/lessons/cancellations`

- Cuerpo: `{ "date": "2026-10-14", "reason": "Lluvia" }`. `reason` es opcional, hasta 200
  caracteres.
- **Cancela todas las clases de ese día del profesor**, en su zona horaria, que no hayan
  empezado ni estén ya canceladas. Cada una con sus reservas, como `cancel` hoy, y **todas en
  una transacción**: o se cancela el día entero o nada.
- **Una fecha pasada** da 422. **Un día sin clases** responde 200 con la lista vacía: no es un
  error, sólo no había nada.
- Responde las clases canceladas, para que la pantalla diga cuántas y a cuántos alumnos se ha
  avisado.
- **El motivo va en el correo y no se guarda.** Es texto libre de Marcos y no hace falta después.
- **La cancelación de una sola clase acepta también el motivo** (aceptado por Daniel): si Marcos
  se pone enfermo, quiere decirlo igual que con la lluvia.
- No crea una excepción de disponibilidad: si escampa, Marcos puede volver a abrir clases ese
  día.

### 5. Métrica

`tennis.mail.sent{type, outcome}`, como las de la Fase 16: cuántos correos salen y cuántos
fallan, sin direcciones.

## Contrato

Todo va a `openapi.yaml` primero:
- `POST /teacher/lessons/cancellations` y el cuerpo opcional `reason` de
  `POST /teacher/lessons/{id}/cancel`;
- `npm run api:types`;
- la fila nueva en `AccessMatrixTest`;
- en `11-contrato-api.md`, el código `DATE_IN_THE_PAST`.

## Frontend

- **Agenda del profesor**: "Cancelar el día" en la cabecera de cada día con clases. El diálogo
  dice cuántas clases y cuántos alumnos afecta, pide el motivo y avisa de que se les escribirá.
- **Cancelar una clase**: el diálogo gana el motivo.
- El alumno no tiene pantalla nueva: el aviso le llega por correo.

Skills:
- **frontend-design**: cancelar un día es la acción con más alcance que tiene Marcos, y el
  diálogo tiene que dejarlo claro antes de confirmar.
- **react-best-practices**: el diálogo de cancelar una clase y el del día comparten el
  formulario del motivo.
- **playwright-cli**: cada pantalla a 390 y a 1440 px.

## Fuera, y por qué

- **Recordatorio antes de la clase**: lo descarta `01` §18.
- **Suscripción al calendario** (una URL `webcal://` con todas las reservas): necesita un token
  en la URL que no caduque, y con el `.ics` de cada correo el alumno ya tiene sus clases.
- **Descargar el `.ics` desde la app**: si falla el correo, la reserva se ve en la app. Encaja en
  la misma fase que una suscripción, si llega.
- **Correos HTML**: el texto plano llega bien a todos los clientes y no hay que mantener
  plantillas.
- **Que Marcos elija qué avisos recibe**: `01` §18 los decidió todos.
- **Reintentos y *outbox***: ver la decisión 1.

## Tests

- **Quién recibe qué**: un test por fila de la tabla de la decisión 2, con un `Mailer` que graba
  en lugar de enviar.
- **Después del commit**:
  - la reserva que pierde la carrera por la última plaza no manda correo;
  - con el SMTP fallando, la reserva queda confirmada.
- **El `.ics`**:
  - la reserva y su cancelación llevan el mismo `UID`, una `REQUEST` y la otra `CANCEL`;
  - una nota con comas, punto y coma y saltos de línea sale escapada;
  - ninguna línea pasa de 75 octetos y todas acaban en `CRLF`;
  - las horas salen en UTC, también el día del cambio de hora (fechas literales, como en
    `availability`).
- **Cancelar el día**:
  - cancela las clases del día y sólo esas, en la zona del profesor: una clase a las 00:30 hora
    de Madrid es de ese día aunque en UTC sea del anterior;
  - deja fuera la que ya empezó y la ya cancelada;
  - si una falla, no se cancela ninguna;
  - la fecha pasada da 422; el alumno y el admin, 403.
- **Sin datos personales**: los logs de envío llevan el tipo y el id de la reserva, nunca la
  dirección.
- **E2E**, con Mailpit (ya está en el stack):
  - el alumno reserva y le llega el correo con el `.ics`;
  - Marcos cancela el día y al alumno le llega la cancelación.

**Mutaciones**, cada una se ve fallar y se deshace:
- escuchar antes del commit en vez de después;
- un `UID` distinto en la cancelación;
- calcular el día en UTC en vez de en la zona del profesor;
- cancelar el día fuera de una transacción.

## Criterios de aceptación

- Existe un test por cada fila de la tabla de destinatarios.
- Existe un test que demuestra que una reserva deshecha no envía correo.
- Existe un test que demuestra que la reserva y su cancelación comparten `UID` en el `.ics`.
- Existe un test que demuestra que cancelar el día usa la zona horaria del profesor.
- Las cuatro mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y los dos E2E nuevos pasan.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Los hechos en `booking` y `lesson`, y el módulo `notification` con el `Mailer` que graba.
3. Las plantillas y el `.ics`.
4. El envío real, en segundo plano, y la métrica.
5. Cancelar el día y el motivo.
6. Los correos de `identity` con la marca nueva.
7. Frontend.
8. E2E y documentación: `02` §4, `11-contrato-api.md` y las trampas nuevas de `CLAUDE.md`.

## Decisiones tomadas al implementar
