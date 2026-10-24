# Fase 21 — Análisis: lista de espera

Criterio de salida (`12-metodologia-trabajo.md`): una plaza liberada se reserva sola al primero
de la lista.

La regla ya está decidida (`01` §18): la plaza liberada se reserva sola al primero de la lista,
que recibe el correo. Si no puede ir, cancela como cualquier reserva.

## Punto de partida

- **Una clase llena** responde 409 `LESSON_FULL` al reservar. `FULL` no se guarda: se deriva de
  contar las reservas confirmadas (`Lesson.statusAt`).
- **La última plaza la protege el cerrojo de la clase** (`LockLesson`, `SELECT ... FOR UPDATE`).
  Reservar lo toma. **Cancelar una reserva, no**: hoy no hace falta, porque liberar una plaza no
  compite con nadie.
- **Una plaza se libera de seis formas**:
  1. el alumno cancela;
  2. el profesor o el admin cancelan esa reserva;
  3. el profesor sube la capacidad (Fase 19);
  4. el profesor deja de gestionar al alumno;
  5. el admin desactiva su cuenta;
  6. el alumno borra su cuenta.

  Las tres últimas cancelan en bloque, con un `UPDATE`, reservas de varias clases a la vez.
- **Los correos** salen de `notification`, que escucha `BookingConfirmed` y `BookingCancelled`
  después del commit (Fase 20).

## Decisiones propuestas

### 1. Quién se apunta, y cuándo

- **Un alumno que podría reservar esa clase si hubiera sitio**: gestionado por el profesor, con
  el email verificado, sin reserva en ella y sin otra clase a esa hora. Son las mismas
  comprobaciones que al reservar, en el mismo orden.
- **Sólo si la clase está llena.** Si hay plaza, se reserva sin más; apuntarse a la lista daría
  409 `LESSON_NOT_FULL`.
- **Hasta que empiece la clase**, y nunca en una cancelada.
- **Sin límite de gente.** El orden es el de llegada.
- **Se sale cuando quiere**, sin ventana: estar en la lista no le cuesta nada a Marcos.

### 2. La plaza pasa al primero en la misma transacción que la libera

- Quien libera la plaza toma antes el cerrojo de la clase. Dentro de la misma transacción, la
  reserva del primero de la lista queda confirmada y él sale de la lista.
- **Así una plaza nunca se queda libre mientras haya alguien esperando.** Quien entre en ese
  momento a reservar espera al cerrojo y se encuentra la clase llena: le toca apuntarse a la
  lista, detrás.
- **Si el primero ya no puede reservarla, se salta y sale de la lista.** Por ejemplo, porque ha
  reservado otra clase a esa hora o ha dejado de estar gestionado. La plaza pasa al siguiente.
- **Subir la capacidad en dos** promociona a dos.
- **Las cancelaciones en bloque** (los casos 4, 5 y 6) promocionan en cada clase afectada. Toman
  los cerrojos en el orden de los ids, para que dos operaciones así no se bloqueen entre ellas.
- **Cancelar la clase** vacía su lista y no promociona a nadie.

La alternativa descartada es un proceso que revise las listas cada minuto. Deja la plaza libre
mientras tanto, y quien llega primero se la quita a quien esperaba.

### 3. El correo

- El promocionado recibe **«Tienes plaza»**, con el `.ics`, como una reserva confirmada.
- **El texto avisa de cómo cancelar** si ya no puede ir.
- **A Marcos le llega como cualquier reserva**, diciendo que viene de la lista de espera.
- **Apuntarse y salir de la lista no mandan correo**: lo ha hecho el alumno y lo ve en la app.

Por dentro es `BookingConfirmed` con un origen nuevo, `WAITING_LIST`. `notification` sólo
cambia el texto.

### 4. Las últimas 24 horas — **decisión tuya**

Quien reserva con menos de 24 horas ya no puede cancelar (`01` §17). Con la lista eso choca: si
una plaza se libera a 3 horas de la clase, el primero de la lista se encuentra reservado sin
haberlo pedido en ese momento, y además sin poder cancelar. Las opciones son:

- **A (te la recomiendo). La lista se cierra 24 horas antes de la clase.** Lo que se libere
  después queda libre para quien la reserve primero, como hoy, y la lista ya no admite a nadie
  más. Nadie se encuentra una clase que no puede soltar, y la regla de las 24 horas no cambia.
  A cambio, una plaza liberada a última hora puede quedarse vacía.
- **B. Se promociona hasta que empiece la clase, y esa reserva sí se puede cancelar.** Se llena
  más, pero una reserva hecha por la lista se salta la regla de las 24 horas, y Marcos puede
  quedarse con un hueco a última hora igual que si no hubiera lista.
- **C. Se promociona hasta que empiece la clase, y no se puede cancelar.** Es lo más simple,
  pero obliga al alumno a algo que no eligió en ese momento.

### 5. Qué se ve en la app

- **El alumno**, en una clase llena, ve «Apuntarme a la lista de espera» en lugar de «Reservar».
  Ya apuntado, ve en qué puesto está («Eres el 2.º») y puede salir.
- **En «Mis reservas»** aparecen también las listas en las que está, aparte.
- **El profesor**, en el detalle de la clase, ve cuántos esperan y quiénes, en orden. Sólo para
  consultar: echar a alguien de la lista queda fuera.
- **El admin** no ve nada nuevo.

### 6. Datos

- Una tabla `waiting_list_entries`, con:
  - `id`, `lesson_id`, `teacher_user_id`, `student_user_id` y `joined_at`;
  - un único por (`lesson_id`, `student_user_id`).
- **La fila se borra** al promocionar, al salir o al cancelar la clase. No hace falta un
  histórico: la reserva que nace de la lista ya cuenta lo que importa.
- **Sólo ids y una fecha.** No copia datos personales, así que borrar la cuenta sólo tiene que
  quitar sus filas, y `DeleteMyAccountTest` no gana columnas que vaciar.
- Va a la lista del `TRUNCATE` de `AbstractIntegrationTest`.
- **Un módulo nuevo no hace falta**: la lista vive en `booking`, que es quien decide quién se
  sienta.

## Contrato

Todo va primero a `openapi.yaml`:
- **`POST /lessons/{id}/waiting-list`**: apuntarse. 201 con el puesto. 409 si la clase no está
  llena (`LESSON_NOT_FULL`), si ya estás (`ALREADY_WAITING`) o si ya tienes plaza.
- **`DELETE /lessons/{id}/waiting-list`**: salir. 204, también si no estabas.
- **`GET /me/waiting-list`**: las listas del alumno, con su puesto en cada una.
- **La vista de una clase** gana `waitingCount`, y para el alumno `waitingPosition` (o `null`).
- **`GET /teacher/lessons/{id}/waiting-list`**: quiénes esperan, en orden.
- Con la opción A, apuntarse a una clase que empieza en menos de 24 horas da 422
  (`WAITING_LIST_CLOSED`).
- `npm run api:types`, las filas nuevas en `AccessMatrixTest` y los códigos nuevos en
  `11-contrato-api.md`.

## Fuera, y por qué

- **Ofrecer la plaza y esperar a que la acepte**: `01` §18 decidió que se reserva sola.
- **Que el profesor reordene la lista o eche a alguien**: con el orden de llegada basta. Si
  Marcos lo echa en falta, se añade después.
- **Avisar al que espera de que ha subido de puesto**: un correo por cada cambio sería ruido;
  lo ve en la app.
- **Bonos**: cuando lleguen (Fase 27), la promoción tendrá que descontar la clase como cualquier
  reserva. Se apunta aquí para que esa fase lo recoja.

## Tests

- **Apuntarse**: las mismas negativas que al reservar (sin verificar, sin gestionar, empezada,
  cancelada, solapada). Además, la clase con plaza da 409, y repetir da 409.
- **Promocionar**, un test por cada una de las seis formas de liberar plaza:
  - el primero recibe la plaza y sale de la lista;
  - el segundo pasa a ser el primero.
- **Se salta a quien ya no puede**: el primero con otra clase a esa hora queda fuera y la plaza
  va al segundo.
- **Subir la capacidad en dos** promociona a dos.
- **Cancelar la clase** vacía la lista sin crear reservas.
- **Concurrencia**, como `LastSeatConcurrencyTest`: un alumno cancela mientras otro intenta
  reservar. La plaza acaba siempre en el primero de la lista y nunca en el que llegó.
- **Las 24 horas**, según lo que decidas en la 4, con `Clock` fijo.
- **Correo**: el promocionado recibe «Tienes plaza» con `.ics`, y Marcos el aviso de la reserva.
- **Borrar la cuenta** quita sus filas de todas las listas.
- **E2E**: un alumno se apunta a una clase llena, otro cancela, y el primero ve la reserva en
  «Mis reservas» y le llega el correo en Mailpit.

**Mutaciones**, cada una se ve fallar y se deshace:
- promocionar después del commit en vez de dentro de la transacción;
- cancelar sin tomar el cerrojo de la clase;
- no saltar al que ya no puede reservar;
- olvidar la promoción en una cancelación en bloque.

## Criterios de aceptación

- Existe un test por cada una de las seis formas de liberar una plaza que demuestra que la recibe
  el primero de la lista.
- Existe un test de concurrencia que demuestra que la plaza no se la lleva quien llega en ese
  momento.
- Existe un test que demuestra que cancelar la clase vacía la lista.
- Existe un test para la regla de las 24 horas elegida en la decisión 4.
- Las cuatro mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. La tabla y apuntarse y salir.
3. La promoción en cancelar una reserva, con el cerrojo, y su test de concurrencia.
4. La promoción en subir la capacidad y en las cancelaciones en bloque; cancelar la clase vacía
   la lista.
5. El correo.
6. Frontend: el botón de la clase llena, el puesto, «Mis reservas» y la lista del profesor.
7. E2E y documentación: `02`, `03`, `10-diagrama-er.md`, `11-contrato-api.md` y las trampas
   nuevas de `CLAUDE.md`.

## Decisiones tomadas al implementar
