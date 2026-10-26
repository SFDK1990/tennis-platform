# Fase 24 — Análisis: mensajes

Criterio de salida (`12-metodologia-trabajo.md`): mensajes de Marcos a un grupo, un nivel o todos,
por correo y en un tablón.

La regla ya está decidida (`01` §18): los mensajes de Marcos van a un grupo, a un nivel o a todos,
y llegan por correo y en un tablón de la app.

## Punto de partida

- **Los correos salen de `notification`**, después del commit y en segundo plano (Fase 20). Hoy
  sólo los dispara `booking`.
- **Los niveles** existen desde la Fase 23, y un alumno tiene uno o ninguno.
- **Un grupo fijo** es una serie semanal de la Fase 22: sus clases comparten `series_id`.
- **La Pi envía con Brevo**, cuyo plan gratuito admite 300 correos al día. Un mensaje a todos son
  tantos correos como alumnos.
- **No hay ningún tablón** en la app: el alumno ve su inicio, el calendario y sus reservas.

## Decisiones propuestas

### 1. A quién se escribe

Marcos elige el destino entre tres:
- **Todos** sus alumnos gestionados.
- **Un nivel**: los alumnos que lo tienen.
- **Un grupo**: decisión 2.

**Los destinatarios se fijan al enviar.** Un alumno que entra después en ese nivel o en ese grupo
no recibe los mensajes anteriores. Cada mensaje guarda a quién fue, y eso es lo que ve cada uno
en su tablón.

### 2. Qué es un grupo — **decisión tuya**

- **A (te la recomiendo). Los alumnos de una clase o de su serie.** Desde la página de una clase
  Marcos tiene «Escribir a los alumnos», con dos opciones: los de esa clase o los de toda la serie
  (cualquiera que tenga reserva en alguna de sus clases futuras). Sirve para «mañana no hay
  clase» y para «el grupo de los martes cambia de pista». No hay que mantener nada aparte.
- **B. Grupos que Marcos crea a mano** («Equipo de competición»), eligiendo alumnos. Es más
  flexible, pero es otra lista que mantener y se solapa con los niveles y las series. Además, la
  Fase 28 (mensualidades) ya traerá grupos fijos con alumnos apuntados.

### 3. El mensaje

- Un **asunto** (hasta 120 caracteres) y un **texto** (hasta 4000), en texto plano, como el resto
  de correos.
- **No se edita.** El correo ya ha salido, y una versión distinta en el tablón confundiría.
- **Marcos lo puede borrar del tablón**, y en ese caso el aviso desaparece para todos. El correo,
  claro, ya no se puede retirar.

### 4. El correo

- Uno por destinatario, firmado por la escuela, con el asunto del mensaje y un enlace al tablón.
- **Sale como los demás**: después del commit y en segundo plano, con el registro sin
  direcciones.
- Un alumno sin email verificado lo ve en el tablón, pero no recibe el correo.
- Si el envío supera el límite diario de Brevo, los correos que fallen se registran sin la
  dirección. El mensaje queda igual en el tablón.

### 5. El tablón

- **El alumno** tiene «Avisos» en el menú: los mensajes que le enviaron, del más reciente al más
  antiguo.
- En su inicio ve el último de los últimos siete días, con un enlace al resto.
- **Sin marca de leído.** Para saber qué ha leído cada uno habría que registrar sus aperturas, y
  Marcos no lo ha pedido.
- **Marcos** tiene «Mensajes»: escribir uno nuevo y la lista de los enviados, con a quién fue cada
  uno y a cuántos alumnos.

### 6. Datos personales

- **El mensaje es de Marcos**, pero cada fila de destinatario apunta a un alumno.
- **Al borrar la cuenta**, sus filas de destinatario se borran: el mensaje sigue en el tablón de
  los demás.
- **La exportación incluye los mensajes que recibió**: asunto, texto y fecha.
- **En los logs** sólo aparecen el id del mensaje y cuántos destinatarios tuvo, nunca el texto.

## Contrato

Todo va primero a `openapi.yaml`:
- **Marcos**:
  - `POST /teacher/messages`, con `{ audience: ALL | LEVEL | LESSON | SERIES, levelId?, lessonId?, subject, body }`.
    Responde 201 con el mensaje y el número de destinatarios.
  - `GET /teacher/messages`: los enviados, paginados.
  - `DELETE /teacher/messages/{id}`.
- **El alumno**: `GET /messages`, sus avisos, paginados.
- Errores:
  - un destino sin alumnos da 422 `NO_RECIPIENTS`: un mensaje que no llega a nadie seguramente
    es un error;
  - un nivel o una clase que no son de Marcos dan 404.
- `npm run api:types`, las filas nuevas en `AccessMatrixTest` y `11-contrato-api.md`.

## Fuera, y por qué

- **Respuestas del alumno o chat**: `01` §18 habla de avisos de Marcos, no de una conversación.
- **Mensajes programados** («envíalo el lunes a las 9»): nadie lo ha pedido.
- **Adjuntos e imágenes**: el texto plano llega bien a todos los clientes de correo.
- **Darse de baja de los avisos**: son comunicaciones del servicio que el alumno contrata, como
  los de reserva. Si Marcos envía publicidad algún día, se revisa.
- **Notificaciones push**: van con la PWA (Fase 30), si llegan.

## Tests

- **Destinatarios**, un test por destino:
  - todos;
  - un nivel, que deja fuera a los de otros niveles y a los que no tienen;
  - una clase;
  - una serie, contando sólo a quien tiene reserva en sus clases futuras.
- **Se fijan al enviar**: un alumno que llega al nivel después no ve el mensaje.
- **Correo**: cada destinatario recibe uno; un alumno sin email verificado, no.
- **Tablón**: cada alumno ve sólo sus mensajes, y uno borrado desaparece.
- **Borrar la cuenta** quita sus filas de destinatario, y la exportación incluye lo recibido.
- **Logs**: el texto del mensaje no aparece.
- **E2E**: Marcos escribe al nivel de un alumno, y el alumno lo ve en su tablón y le llega el
  correo en Mailpit.

**Mutaciones**, cada una se ve fallar y se deshace:
- resolver los destinatarios al leer en lugar de al enviar;
- no filtrar por nivel;
- mandar correo a un alumno sin verificar.

## Criterios de aceptación

- Existe un test por cada destino.
- Existe un test que demuestra que los destinatarios se fijan al enviar.
- Existe un test que demuestra que un alumno no ve los mensajes de otros.
- Las mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Tablas `messages` y `message_recipients`, y los destinatarios.
3. Enviar, listar y borrar.
4. El correo, en `notification`.
5. Borrado de la cuenta y exportación.
6. Frontend: «Mensajes» de Marcos, «Escribir a los alumnos» en la clase, y el tablón del alumno.
7. E2E y documentación: `02`, `03`, `10-diagrama-er.md`, `11-contrato-api.md` y `CLAUDE.md`.

## Decisiones tomadas al implementar
