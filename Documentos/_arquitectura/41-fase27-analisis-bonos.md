# Fase 27 — Análisis: bonos

Criterio de salida (`12-metodologia-trabajo.md`): tipos de bono de Marcos. El pago se apunta, pero
no se cobra en la app.

La regla ya está decidida (`01` §18):
- La clase se descuenta al reservar, y se devuelve si se cancela a tiempo (con más de 24 horas).
- Si el alumno no viene, la pierde.
- Sin saldo no se reserva.
- Cada tipo de bono tiene el precio y la caducidad (o ninguna) que ponga Marcos.

## Punto de partida

- **Hoy reservar no cuesta nada**: cualquier alumno gestionado y verificado reserva si hay plaza.
- **Una plaza sale de cinco sitios**:
  - el alumno la reserva;
  - la lista de espera se la da (Fase 21);
  - el alumno cancela;
  - Marcos o el admin cancelan la reserva o la clase entera;
  - borrar o desactivar la cuenta cancela las futuras.
  Cada uno tendrá que mover el saldo, y `WaitingListApiTest` ya enseña cómo probar «un test por
  camino».
- **Las mensualidades (Fase 28)** pagarán por otro lado las clases de un grupo fijo.

## Decisiones propuestas

### 1. Para qué clases sirve cada bono — **A** (Daniel, 30/10/2026)

- **A (te la recomiendo). Cada tipo de bono es para clases de grupo o para particulares**, porque
  no cuestan lo mismo. Por ejemplo, «10 clases de grupo, 120 €, 3 meses» o «5 particulares, 150 €,
  sin caducidad».
- **B. Un bono vale para cualquier clase**, y una clase es una clase.

### 2. Cuándo empieza a pedirse bono — **A** (Daniel, 30/10/2026)

Al desplegar, nadie tiene bono. Si «sin saldo no se reserva» se aplicara desde ese momento, nadie
podría reservar.

- **A (te la recomiendo). Marcos lo activa en su configuración cuando tenga los bonos dados de
  alta.** Hasta entonces se reserva como ahora.
- **B. Se aplica al desplegar**, y Marcos da los bonos de alta antes de desplegar.

### 3. Cómo llega un bono a un alumno

- **Marcos lo apunta en la ficha del alumno**: qué tipo, cuándo lo pagó y cómo (efectivo, Bizum,
  transferencia). El precio y la caducidad salen del tipo.
- La app no cobra. Corregir un bono apuntado por error es anularlo, y sólo se puede si aún no se ha
  usado ninguna clase.
- **El alumno ve su saldo** en «Mis reservas» y en Inicio: «Te quedan 4 clases de grupo, hasta el 15
  de enero».

### 4. De qué bono sale cada clase

- **Del que caduca antes**, para que no se pierdan clases. Uno sin caducidad, el último.
- **Un bono caducado ya no sirve**, aunque le queden clases. Las clases que ya reservó con él siguen
  siendo suyas.

### 5. Los cinco caminos de una plaza

- **Reservar** descuenta una clase. Sin saldo para ese tipo de clase, 422 `NO_PASS_BALANCE`.
- **Apuntarse a la lista de espera** pide saldo, pero no lo descuenta. Si al llegar su turno ya no le
  queda, la plaza pasa al siguiente y él sale de la lista con un correo que lo explica.
- **El alumno cancela** a tiempo y recupera la clase. Dentro de las 24 horas no puede cancelar, como
  ahora.
- **Marcos o el admin cancelan** (la reserva, la clase o un día de lluvia): la clase siempre vuelve,
  aunque falten menos de 24 horas.
- **Desactivar o borrar la cuenta** cancela las reservas futuras y devuelve sus clases.
- **Si no viene**, la clase está gastada desde que reservó: no hay nada que mover.
- La clase vuelve al mismo bono del que salió, aunque ya haya caducado. Así no se recupera más de lo
  que se pagó.

### 6. Lo que ve Marcos

- **Tipos de bono** en «Horario» o en su perfil: nombre, clases, tipo de clase, precio y caducidad.
  Se pueden retirar, pero no borrar si algún alumno tiene uno.
- **En la ficha**, los bonos del alumno, con su saldo y su historial de clases.
- **En la lista de alumnos**, quién se ha quedado sin saldo o lo tiene a punto de caducar.
- **El resumen del mes** gana una línea: bonos vendidos y lo que suman. Es lo único de dinero que
  sabe la app.

## Contrato

Todo va primero a `openapi.yaml`:
- **Tipos de bono**:
  - `GET /pass-types`, para cualquiera con sesión;
  - `POST`, `PATCH` y `DELETE /teacher/pass-types`, sólo el profesor.
- **Los bonos de un alumno**:
  - `GET /teacher/students/{userId}/passes` y `POST` para apuntar uno;
  - `POST /teacher/passes/{id}/void`, para anularlo;
  - `GET /me/passes`, para el alumno.
- **La configuración** gana el interruptor de la decisión 2, si es A.
- **La reserva** dice de qué bono salió.
- **Códigos nuevos**:
  - `NO_PASS_BALANCE` (422);
  - `PASS_TYPE_IN_USE` (409);
  - `PASS_ALREADY_USED` (409), al anular uno que ya se ha usado.
- `npm run api:types`, las filas nuevas en `AccessMatrixTest` y `11-contrato-api.md`.

## Fuera, y por qué

- **Cobrar en la app** (tarjeta o Bizum): es la Fase 27.5; aquí sólo se apunta el pago.
- **Facturas**: también aparcadas.
- **Que el alumno compre el bono desde la app**: llega con el cobro, en la 27.5. Sin cobro sería
  otra forma de apuntar algo que sólo Marcos sabe si ha cobrado.
- **Bonos compartidos entre alumnos**: no se han pedido.

## Tests

- **Un test por camino de la decisión 5**, como `WaitingListApiTest`:
  - reservar descuenta;
  - sin saldo, 422;
  - cancelar a tiempo devuelve;
  - Marcos, el admin y la lluvia devuelven siempre;
  - desactivar y borrar la cuenta devuelven;
  - la lista de espera salta a quien se quedó sin saldo.
- **Elegir el bono**:
  - sale del que caduca antes;
  - uno caducado no sirve;
  - la clase vuelve a su bono.
- **Concurrencia**: dos reservas a la vez con una sola clase de saldo dejan una reserva, no dos. El
  saldo se bloquea igual que la última plaza.
- **El interruptor**: apagado, se reserva sin bono; encendido, no.
- **Anular** un bono sin usar, sí; uno usado, 409.
- **E2E**: Marcos apunta un bono, el alumno reserva, ve su saldo bajar y lo recupera al cancelar.

**Mutaciones**, cada una se ve fallar y se deshace:
- no devolver la clase cuando cancela Marcos;
- usar un bono caducado;
- quitar el bloqueo del saldo;
- devolver la clase al cancelar dentro de las 24 horas.

## Criterios de aceptación

- Existe un test por cada camino y cada regla de arriba.
- Las mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Tipos de bono y bonos de los alumnos: tablas y alta.
3. El saldo en cada camino de la reserva, con su bloqueo.
4. El interruptor, si la decisión 2 es A.
5. Lo que ve Marcos: ficha, lista y resumen.
6. Frontend del alumno: el saldo y el aviso al no tener.
7. E2E y documentación.

## Decisiones tomadas al implementar

- **El saldo no se guarda: se cuenta.** Lo que queda de un bono son sus clases menos sus reservas
  `CONFIRMED` que lo apuntan (`bookings.pass_id`). Así cualquier cancelación, por el camino que
  sea, devuelve la clase sin que nadie tenga que acordarse, y un «no vino» sigue gastada porque la
  asistencia va en su propia columna. Un camino nuevo que *dé* una plaza, en cambio, tiene que
  preguntar a `ChargePasses`, igual que a `LevelRule`.
- **El bono copia del tipo** el nombre, el tipo de clase, las clases, el precio y la caducidad al
  apuntarse. Cambiar el tipo después no reescribe lo vendido; retirarlo sólo impide venderlo.
- **La caducidad se cuenta desde el día del pago**, en meses, y el bono vale para las clases hasta
  ese día incluido, en la zona horaria de Marcos. Lo que mira es el día de la clase, no el de la
  reserva: un bono que caduca el 15 no paga una clase del 16 reservada el 10.
- **El saldo se bloquea** con `SELECT ... FOR UPDATE` sobre los bonos del alumno y su tipo de clase,
  dentro de la transacción de la reserva (`ChargePasses.charge` es `MANDATORY`).
  `PassBalanceConcurrencyTest` falla si se lee sin cerrojo; se comprobó.
- **El interruptor vive en el módulo `pass`** (`pass_settings`), no en `platform_configuration`:
  es de Marcos, no de la plataforma. Se lee con `GET /pass-types` (`required`) y se cambia con
  `PUT /teacher/pass-settings`.
- **El contrato cambió un poco respecto al análisis**:
  - los bonos se leen con `GET /teacher/passes`, con `studentUserId` opcional. Sin él devuelve los
    de todos, que es lo que necesita la lista de alumnos para marcar quién está sin saldo o a punto
    de caducar (14 días), en una petición;
  - apuntar uno es `POST /teacher/students/{userId}/passes`, como estaba previsto;
  - salen cuatro códigos más: `PASS_TYPE_NOT_FOUND`, `PASS_NOT_FOUND`, `PASS_TYPE_ALREADY_EXISTS` y
    `PASS_TYPE_RETIRED`.
- **Marcos tiene una pantalla «Bonos»** en el menú lateral, con los tipos, el interruptor y los
  bonos de todos, y la lista de alumnos enlaza a ella. En «Horario» o en el perfil se perdía.
- **La lista de espera salta a quien no tiene saldo** y le escribe (`waiting-place-lost-student`,
  «Sin bono para la plaza»). Apuntarse ya lo pide, pero el saldo puede gastarse mientras espera.
- **La exportación de datos del alumno** incluye sus bonos, y cada reserva, el bono con el que se
  pagó.
- **El resumen del mes** suma los bonos por el día del pago, no por el de alta.
- **La cuarta mutación no se puede hacer.** «Devolver la clase al cancelar dentro de las 24 horas»
  no tiene dónde ir: el alumno no puede cancelar dentro de las 24 horas (`CANCELLATION_WINDOW_EXPIRED`), y
  cuando cancela Marcos la clase vuelve siempre. Las otras tres se vieron fallar:
  - contar las canceladas por Marcos como gastadas rompe dos tests;
  - quitar la caducidad rompe el del bono caducado;
  - leer sin cerrojo rompe el de concurrencia.
  Además, ignorar la falta de saldo en la promoción rompe el de la lista de espera.
- **Un riesgo que queda documentado**: borrar o desactivar una cuenta bloquea sus clases una a una
  y promueve a otros alumnos, lo que bloquea los bonos de esos alumnos. Si a la vez uno de ellos
  reserva una de esas clases, PostgreSQL puede detectar un interbloqueo y abortar una de las dos,
  que da un error y se reintenta. Es muy raro con un profesor y dos pistas, y no deja datos mal.
