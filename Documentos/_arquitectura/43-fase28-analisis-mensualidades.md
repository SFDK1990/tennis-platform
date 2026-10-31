# Fase 28 — Análisis: mensualidades

Criterio de salida (`12-metodologia-trabajo.md`): cuota por grupo fijo y registro de quién ha
pagado. Un alumno con la cuota vencida sigue activo, y Marcos ve un aviso de que se le pasó la fecha
(Daniel, 27/10/2026).

La regla ya está decidida (`01` §18): las mensualidades van por grupo fijo. El alumno se apunta al
grupo, paga la cuota del mes y sus clases del grupo quedan reservadas. La cuota **se apunta, no se
cobra en la app** (`01` §16).

## Punto de partida

- **Un grupo fijo, hoy, es una serie semanal de la Fase 22**: sus clases comparten `series_id`. No
  hay tabla de series; la serie son las clases que comparten el id. Dura como mucho 16 semanas.
- **Los mensajes de la Fase 24 ya hablan de «el grupo»**: los alumnos con plaza en una clase de la
  serie que aún no ha empezado. Se calcula de las reservas, nadie está «apuntado» a nada.
- **Una plaza sale de cinco sitios** y cada uno ya pregunta a `LevelRule` y a `ChargePasses`
  (Fase 27). Una reserva del grupo es un sexto camino que *da* plaza: tiene que preguntar al nivel y
  no tiene que pedir bono, porque la paga la cuota.
- **El bono ya apunta pagos** (`PaymentMethod`: efectivo, Bizum, transferencia) y sabe anular uno
  apuntado por error. La cuota hace lo mismo, por meses.
- **El resumen del mes ya suma lo que se vende en bonos**: es lo único de dinero que sabe la app.

## Decisiones propuestas

### 1. Qué es un grupo fijo

- **A (te la recomiendo). Un grupo con nombre y cuota, al que Marcos engancha una o varias series.**
  Por ejemplo, «Martes y jueves 18:00, intermedio, 60 € al mes», con la serie de los martes y la de
  los jueves. Al empezar el cuatrimestre siguiente, Marcos crea las series nuevas y las engancha al
  mismo grupo: sus alumnos quedan apuntados a las clases nuevas sin volver a apuntarse.
- **B. Cada serie es un grupo**, con su cuota. Es una tabla menos, pero un grupo de dos días a la
  semana serían dos cuotas, y cada cuatrimestre habría que volver a apuntar a todos.

A cuesta una tabla pequeña (grupo y sus series) y es como Marcos habla de sus grupos.

### 2. Quién apunta al alumno al grupo

- **A (te la recomiendo). Los dos.** El alumno se apunta desde el detalle de una clase del grupo
  («Apuntarme al grupo, 60 € al mes»), y Marcos puede apuntar a cualquiera desde la ficha. Al
  desplegar, los grupos ya existen en la pista: Marcos los pasa a la app sin pedir a nadie que entre.
- **B. Sólo Marcos.** El alumno ve su grupo y sus cuotas, pero no se apunta él.

### 3. Cuándo vence la cuota

- **A (te la recomiendo). Un día del mes que pone Marcos**, el mismo para todos los grupos (por
  defecto el 5). Pasado ese día sin pago apuntado, la cuota está vencida y Marcos ve el aviso.
- **B. El día de la primera clase del mes** de cada grupo. Es más justo con quien empieza tarde, pero
  cada grupo vence un día distinto y el aviso es más difícil de seguir.

### 4. Las plazas que sobran en una clase del grupo

- **A (te la recomiendo). Se reservan sueltas**, como hoy, con bono si el interruptor de la Fase 27
  está encendido, y entran en la lista de espera. Un grupo de seis con cuatro apuntados deja dos
  plazas que Marcos puede llenar.
- **B. Sólo para los del grupo.** Las plazas libres se pierden.

Con A, una reserva suelta en una clase futura ocupa una plaza que quien quiera apuntarse al grupo
también necesita: apuntarse pide sitio en todas las clases que quedan (ver «Reglas»).

### 5. Darse de baja

- **A (te la recomiendo). La baja libera las clases desde el mes siguiente.** Las del mes en curso
  se quedan, porque ya están pagadas o se deben; si no va a alguna, la cancela como cualquier
  reserva.
- **B. La baja cancela todas las clases futuras desde ya**, y la cuota del mes en curso la resuelve
  Marcos a mano.

## Reglas (se desarrollan de §18, no se preguntan)

**Apuntarse:**
- Lo hace un alumno gestionado y verificado, como al reservar, y el nivel cuenta (`LevelRule`): el
  grupo tiene el nivel de sus clases.
- Reserva una plaza en cada clase del grupo que aún no ha empezado, en una sola transacción y con el
  cerrojo de cada clase (`LockLesson`). Si alguna está llena, no se apunta y el error dice qué fechas
  (409 `GROUP_FULL`); Marcos puede subir la capacidad o quitar la reserva suelta.
- Esas reservas llevan el grupo (`bookings.membership_id`) y no piden bono.
- Una serie que se engancha después reserva su plaza a todos los apuntados, con la misma regla.
- **Un solo correo** al alumno («Te has apuntado al grupo…», con las fechas) y uno a Marcos, no uno
  por clase.

**Cada clase:**
- El apuntado cancela una clase como cualquier reserva (más de 24 horas), y la plaza pasa a la lista
  de espera (`WaitingListPromotion`). Si cambia de idea, la vuelve a reservar mientras haya sitio, y
  sigue siendo del grupo.
- La cuota no baja por una clase cancelada, la cancele quien la cancele (también la lluvia). Si
  Marcos quiere compensar, apunta menos importe o no cobra ese mes.

**La cuota:**
- Se debe cada mes, en la zona horaria de Marcos, en el que el alumno está apuntado y el grupo tiene
  alguna clase. Quien se apunta a mitad de mes debe ese mes; lo que cobre es cosa de Marcos.
- **Marcos apunta el pago** desde la pantalla de mensualidades o la ficha: mes, importe (por defecto,
  la cuota del grupo), día y forma de pago. También puede marcar un mes como «no se cobra»
  (vacaciones, una lesión).
- Un pago apuntado por error se anula; sólo hay un pago vivo por alumno, grupo y mes.
- **Vencida no bloquea nada**: el alumno sigue apuntado y con sus clases. Marcos ve el aviso en «Hoy»,
  en la lista de alumnos y en la ficha.
- El alumno ve sus grupos y sus cuotas en «Mis reservas»: «Noviembre: pagada» o «Noviembre:
  pendiente».
- **Cambiar la cuota del grupo** vale desde el mes siguiente; lo ya apuntado no se reescribe.

**La baja**, la desactivación y el borrado de la cuenta cancelan las reservas del grupo por los
caminos de siempre, que ya devuelven bono y promueven la lista de espera. Borrar la cuenta vacía los
datos personales como hasta ahora (`DeleteMyAccountTest`): las cuotas no guardan ninguno.

**Lo que ve Marcos:**
- **Una pantalla «Mensualidades»** en el menú lateral: sus grupos (nombre, series, nivel, cuota,
  apuntados y plazas) y, mes a mes, quién ha pagado, quién no y quién la tiene vencida.
- **En la ficha**, los grupos del alumno y sus cuotas.
- **El día de vencimiento** en esa misma pantalla.
- **El resumen del mes** gana una línea: cuotas cobradas y lo que suman, junto a la de bonos.

## Contrato

Todo va primero a `openapi.yaml`:
- **Grupos**:
  - `GET /groups`, para cualquiera con sesión: nombre, nivel, cuota, días y horas, plazas;
  - `POST`, `PATCH` y `DELETE /teacher/groups`, y `POST /teacher/groups/{id}/series` para
    engancharle una serie. Un grupo con apuntados no se borra (409 `GROUP_IN_USE`).
- **Apuntarse y darse de baja**:
  - `PUT` y `DELETE /me/groups/{id}/membership`, el alumno;
  - `PUT` y `DELETE /teacher/groups/{id}/members/{userId}`, Marcos;
  - `GET /me/memberships`, con sus cuotas.
- **Cuotas**:
  - `GET /teacher/fees?month=2026-11`, con el estado de cada apuntado (`PAID`, `WAIVED`, `PENDING`,
    `OVERDUE`); `OVERDUE` se deriva al leer, no se guarda;
  - `POST /teacher/fees` para apuntar un pago o un «no se cobra», y `POST /teacher/fees/{id}/void`;
  - `PUT /teacher/fee-settings` con el día de vencimiento.
- **La reserva** dice si es del grupo.
- **Códigos nuevos**: `GROUP_NOT_FOUND` (404), `GROUP_FULL` (409), `ALREADY_IN_GROUP` (409),
  `NOT_IN_GROUP` (404), `GROUP_IN_USE` (409), `SERIES_ALREADY_IN_GROUP` (409), `FEE_ALREADY_PAID`
  (409), `FEE_NOT_FOUND` (404).
- `npm run api:types`, las filas nuevas en `AccessMatrixTest` y `11-contrato-api.md`.

**Tablas nuevas**, en un módulo nuevo `membership`: `fixed_groups`, `fixed_group_series` (una serie
en un solo grupo), `group_memberships` (alumno, desde, hasta) y `monthly_fees` (mes, importe, día,
forma, estado). `bookings` gana `membership_id`. Todas entran en el `TRUNCATE` de los tests, y la
exportación de datos del alumno incluye sus grupos y sus cuotas.

## Fuera, y por qué

- **Cobrar la cuota en la app**: `01` §16 dice que se apunta. Si un día se quiere, el puerto
  `PaymentGateway` de la 27.5 ya está y sería una fase pequeña.
- **Recordatorios por correo** de la cuota vencida: no se han pedido, y §18 no quiere recordatorios.
- **Prorratear solo** la cuota de quien entra a mitad de mes: Marcos pone el importe al apuntarlo.
- **Descuentos por hermanos**: eran de Familias, descartada.
- **Facturas**: siguen aparcadas.

## Tests

- **Apuntarse**:
  - reserva todas las clases futuras del grupo, y ninguna empezada;
  - con una llena, 409 `GROUP_FULL` y ninguna reserva;
  - con otro nivel, rechazado; sin verificar o sin gestionar, como al reservar;
  - con el interruptor de bonos encendido, no gasta bono;
  - dos veces, 409; Marcos apunta a un alumno igual que el alumno se apunta.
- **Enganchar una serie** reserva a todos los apuntados; una serie ya enganchada, 409.
- **Cada clase**: cancelar una a tiempo libera la plaza y la lista de espera la da; volver a
  reservarla la deja del grupo.
- **La baja** libera desde el mes siguiente (decisión 5) y promueve la lista de espera; la
  desactivación y el borrado también.
- **Las cuotas**:
  - el mes se debe con una clase en él, y no sin ninguna;
  - vencida pasado el día, pendiente antes, con `Clock` fijo y los meses en hora de Marcos;
  - un segundo pago del mismo mes, 409; anulado, se puede volver a apuntar;
  - «no se cobra» no sale como vencida;
  - cambiar la cuota no reescribe lo apuntado.
- **Concurrencia**: dos alumnos que se apuntan a la vez por la última plaza dejan uno dentro, no
  dos. El cerrojo de la clase ya lo garantiza; el test lo comprueba.
- **E2E**: Marcos crea el grupo con una serie, el alumno se apunta y ve sus clases reservadas y la
  cuota pendiente; Marcos apunta el pago y el aviso desaparece.

**Mutaciones**, cada una se ve fallar y se deshace:
- pedir bono a una reserva del grupo;
- apuntarse con una clase llena;
- contar como vencida una cuota marcada «no se cobra»;
- quitar la promoción de la lista de espera en la baja.

## Criterios de aceptación

- Existe un test por cada regla de arriba.
- Las mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Módulo `membership`: grupos, sus series y el día de vencimiento.
3. Apuntarse y la baja, con sus reservas, el nivel y el cerrojo.
4. Cuotas: apuntar, anular, «no se cobra» y el estado del mes.
5. Lo que ve Marcos: «Mensualidades», ficha, «Hoy», lista de alumnos y resumen.
6. Frontend del alumno: apuntarse desde la clase y sus cuotas en «Mis reservas».
7. E2E y documentación.
