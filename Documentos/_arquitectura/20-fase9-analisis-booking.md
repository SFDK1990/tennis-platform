# Fase 9 — Análisis: las reservas (`booking`)

Análisis previo a escribir código, como las fases 6, 7 y 8. Es la Fase 5 de
`12-metodologia-trabajo.md`, que numera distinto que la tabla de `12-metodologia-trabajo.md`.

`booking` es la ruta crítica de concurrencia del MVP, y además hereda cuatro piezas que la Fase 8
no podía escribir porque sólo este módulo sabe contar reservas: `bookedCount`, el estado `FULL`,
el marcado de asistencia y la cascada al cancelar una clase. A esas cuatro se suma una quinta que
arrastra la Fase 6: **desactivar un alumno todavía no cancela sus reservas futuras**, y el propio
javadoc de `ManageStudent.stopManaging` dice que esta fase debe cerrarlo con un test.

Este documento se validó antes de escribir código, con las siete decisiones del apartado
"Decisiones cerradas antes de implementar" acordadas explícitamente el 26/09/2026.

## Alcance

El criterio de salida del roadmap, en su forma medible (regla adicional 5 de
`12-metodologia-trabajo.md`): **existe un test que lanza dos reservas simultáneas de la última
plaza de una clase y demuestra que exactamente una gana, la otra recibe `409 LESSON_FULL`, y
ninguna termina en un 500.**

Entra:

- Reservar una clase (alumno), con todas las comprobaciones de `01-analisis-funcional.md` §9.
- Listar reservas: las propias (alumno) y las de las clases del profesor (profesor).
- Cancelar una reserva: el alumno la suya, dentro de la ventana de 24 horas; el profesor
  cualquiera de sus clases.
- Marcar asistencia.
- Las cuatro piezas heredadas de la Fase 8 y la deuda de la Fase 6.
- El changeset de `bookings` y sus restricciones.

No entra:

- Modificar una clase con reservas. `01-analisis-funcional.md` §8 ya dice que la operación
  recomendada es cancelar y crear otra, y la Fase 8 lo confirmó.
- Notificaciones de ningún tipo (fuera del MVP según §16).
- Lista de espera, que ningún documento pide.

## El problema de fondo: tres operaciones que van contra el grafo

Esta es la decisión que condiciona todas las demás, así que va antes que ellas.

El grafo dice `booking → lesson` y `booking → student`. Pero hay tres cosas que tienen que ocurrir
**en sentido contrario**:

1. **Cancelar una clase cancela sus reservas.** La operación empieza en `lesson` y tiene que
   escribir en `booking`.
2. **Desactivar un alumno cancela sus reservas futuras.** Empieza en `student` y tiene que escribir
   en `booking`.
3. **Leer una clase dice cuántas plazas tiene ocupadas y si está `FULL`.** La lectura la sirve
   `lesson` y el dato lo tiene `booking`.

Ninguna de las tres se resuelve añadiendo una arista, porque cualquiera cierra un ciclo, y
`modulesAreFreeOfCycles` lo rechaza con razón. Las opciones reales son tres:

### Opción A — `booking` orquesta

`booking` sirve él mismo `POST /teacher/lessons/{id}/cancel` y `DELETE /teacher/students/{id}`:
llama a `CancelLesson` o a `ManageStudent.stopManaging` y cancela las reservas en la misma
transacción. Respeta el grafo sin tocarlo.

Funciona para las dos cascadas y **no funciona para la tercera**: para que `GET /lessons/{id}`
lleve `bookedCount`, `booking` tendría que servir también las lecturas de clases, y `lesson` se
quedaría sin adaptador web. Además, obliga a mover endpoints que ya existen y ya tienen tests a
otro módulo, y cualquier llamador futuro de `CancelLesson` —la consola de `administration`, por
ejemplo— se saltaría la cascada sin enterarse.

### Opción B — eventos de Spring en el mismo proceso

`lesson` publica un `LessonCancelled` y `student` un `StudentDeactivated`; `booking` los escucha
con un `@EventListener` síncrono, que corre dentro de la transacción del publicador.

Resuelve las cascadas con el grafo intacto, y **tampoco sirve para la tercera**: un evento no
devuelve un recuento. Y tiene un defecto propio: el acoplamiento sigue ahí, pero ya no se ve. Nada
en `CancelLessonService` dice que cancelar una clase toca reservas, y un listener que deje de
registrarse no falla — la cascada simplemente deja de ocurrir.

### Opción C — inversión de dependencias, que es la acordada

`lesson` y `student` **declaran la interfaz que necesitan** y `booking` **la implementa**:

- `lesson` declara algo como `LessonBookings` con dos operaciones: contar las reservas activas de
  un conjunto de clases, y cancelar las de una clase que se cancela.
- `student` declara algo como `StudentBookings` con una: cancelar las reservas futuras de un alumno
  que se desactiva.

En tiempo de compilación, `booking` depende de `lesson` y de `student` —que es exactamente lo que
el grafo ya permite— y en tiempo de ejecución la llamada va en sentido contrario. Es el principio
de inversión de dependencias que `12-metodologia-trabajo.md` lista entre los principios del
proyecto, aplicado a su caso de manual.

Frente a A y B, esta opción:

- **Resuelve las tres cosas con un solo mecanismo**, incluida la que ninguna de las otras dos
  resuelve.
- **Deja el acoplamiento a la vista.** `CancelLessonService` recibe `LessonBookings` en su
  constructor; quien lo lea sabe que cancelar toca reservas.
- **Falla al arrancar, no en silencio.** Si nadie implementa la interfaz, el contexto no levanta.
- **Es atómica sin esfuerzo**: la implementación de `booking` corre dentro de la transacción de
  `CancelLessonService`, y si falla, la cancelación de la clase se deshace con ella.
- **La hereda cualquier llamador futuro de `CancelLesson`**, porque la cascada vive en el caso de
  uso y no en el endpoint.

#### Lo que cuesta: una regla de frontera se amplía

`crossesOnlyThroughInboundPorts` dice que un módulo sólo puede tocar los `port.in` de otro. Una
interfaz que `lesson` declara para que otro la implemente es, por definición, un puerto de
**salida** de `lesson`, y hoy `booking` no puede verla.

Hay dos formas de dar cabida a esto, y se eligió la segunda:

- Meter las interfaces en `port.in` de `lesson` y `student`. No toca ArchUnit, pero miente: un
  puerto de entrada es lo que un módulo ofrece, y esto es lo que un módulo pide.
- **Crear un paquete explícito para ello, `application.port.provided`** o similar —el nombre se
  decide al implementar—, y ampliar la regla para que otro módulo pueda **implementar** sus
  interfaces, no llamarlas. La regla sigue siendo mecánica y la excepción tiene nombre propio,
  en lugar de ser un hueco en una regla general.

El javadoc de la regla tendrá que explicar esto con el mismo cuidado que el de la arista
`→ identity`: por qué existe, y por qué no abre la puerta a nada más.

## Decisiones

### Bloquear la clase: `lesson` presta el cerrojo

La capacidad no se puede expresar como restricción declarativa, porque exige contar filas de otra
tabla. `02-arquitectura.md` §11 y `03-modelo-de-datos.md` §5 ya fijan el método: **bloquear la
fila de la clase con `SELECT ... FOR UPDATE`, contar, comparar e insertar, todo en una
transacción**. Dos reservas de la misma clase se serializan en el cerrojo; la segunda cuenta
después de que la primera haya insertado.

El problema es que la fila está en `lessons`, y `booking` no puede tocar el repositorio de
`lesson`. Propongo que `lesson` exponga un puerto de entrada que **lea la clase bloqueándola** y
devuelva su `LessonView`, y que exija estar dentro de una transacción
(`Propagation.MANDATORY`): un cerrojo que se suelta al volver del método no protege nada, y
`MANDATORY` convierte ese error en una excepción en vez de en una carrera.

**Sin `@Version` y sin bloqueo optimista.** La Fase 8 dejó la columna `version` sin mapear en
espera de esta fase. Con un bloqueo pesimista sobre la única fila que importa, el optimista no
añade protección y sí un segundo camino de error —`OptimisticLockException`— que habría que
traducir. La columna se queda como está, sin mapear, y el comentario de `LessonEntity` que la
anuncia para la Fase 9 se corrige.

### El solapamiento del alumno: restricción de exclusión, igual que el del profesor

`03-modelo-de-datos.md` proponía bloquear la fila del alumno y comprobar en la aplicación.
`10-diagrama-er.md`, posterior, cambió a un `EXCLUDE USING gist` sobre una **copia** de los
instantes de la clase en la reserva, porque una restricción de exclusión no puede hacer `JOIN`.
Propongo seguir `10-diagrama-er.md`, por la misma razón que en la Fase 8: **comprobar dos veces a
propósito**, antes en la aplicación para responder un `409 STUDENT_SCHEDULE_OVERLAP` con sentido,
y en la base porque el mismo alumno puede enviar dos peticiones a la vez
(`01-analisis-funcional.md` §14 lo lista como caso límite).

La copia de los instantes es segura **porque la modificación de clases no existe**. El día que
exista, tendrá que actualizar esas copias en la misma transacción; lo dejo escrito en el
changeset para que no sea una sorpresa.

La reserva duplicada, igual: índice único parcial `(lesson_id, student_user_id) WHERE status =
'CONFIRMED'`, comprobación previa para el mensaje, y la violación traducida al mismo `409
BOOKING_ALREADY_EXISTS`. La lección de la Fase 8 se aplica desde el principio: **se traduce por
SQLState y nombre de restricción**, no por el texto del mensaje, y hay un test que va directo al
repositorio para demostrar que la restricción existe y no la está tapando la comprobación previa.

### Sin trigger de capacidad

`10-diagrama-er.md` sugiere, como opcional, un trigger que recuente reservas tras cada `INSERT` y
aborte si se supera la capacidad. Propongo **no ponerlo**. A diferencia de los solapamientos, aquí
no hay carrera que tapar: el único camino que aumenta el número de reservas confirmadas pasa por
el cerrojo, y el test de concurrencia es el que lo demuestra. Un trigger duplicaría la regla en
PL/pgSQL, y de las dos copias la que no se lee es la que se desincroniza.

### La asistencia va en su propia columna — corrige `10-diagrama-er.md`

Hay dos modelos escritos y se contradicen:

- `01-analisis-funcional.md` §12 y `03-modelo-de-datos.md`: el estado de la reserva
  (`CONFIRMED` o cancelada) y la asistencia (`PENDING`, `ATTENDED`, `NO_SHOW`) son **dos campos**.
- `10-diagrama-er.md`, `01-analisis-funcional.md` y `openapi.yaml`: **un solo campo** con
  `ATTENDED` y `NO_SHOW` entre los valores de `bookings.status`.

Propongo el primero, y lo justifico porque cambia el contrato. Si `ATTENDED` es un valor de
`status`, marcar asistencia **deja de hacer que la reserva sea `CONFIRMED`**, y todo lo que se
escribió sobre `status = 'CONFIRMED'` deja de funcionar a la vez:

- El índice único parcial ya no ve la reserva: el mismo alumno podría volver a reservar esa clase.
- La restricción de exclusión tampoco: el alumno queda libre para otra clase a esa hora.
- El recuento de plazas baja al marcar asistencia, y una clase `FULL` se lee `OPEN`.

Se puede arreglar cambiando cada `WHERE` a `status IN ('CONFIRMED','ATTENDED','NO_SHOW')`, y es
exactamente la clase de detalle que alguien olvida en la cuarta consulta. Es el mismo argumento
que llevó a la Fase 8 a no guardar `FULL`: **dos hechos distintos en una columna acaban
contradiciéndose**. Que el alumno cancelara y que el alumno viniera son dos preguntas, y cada una
tiene su columna.

Resultado propuesto: `status` ∈ {`CONFIRMED`, `CANCELLED_BY_STUDENT`, `CANCELLED_BY_TEACHER`,
`CANCELLED_BY_ADMIN`} y `attendance` ∈ {`PENDING`, `ATTENDED`, `NO_SHOW`}, con `PENDING` por
defecto. `MarkAttendanceRequest` no cambia de forma; `Booking` gana un campo `attendance`, y
`BookingStatus` pierde dos valores.

### Qué puede reservar un alumno

Las condiciones de `01-analisis-funcional.md` §9, con lo que cada una significa en el código:

| Condición | Cómo se comprueba | Si falla |
|---|---|---|
| Autenticado, verificado, cuenta activa | El token. `LoginService` ya no emite sesión si `canAuthenticate()` es falso | `401` |
| Rol `STUDENT` | El token | `403` |
| Gestionado **por el profesor de esa clase**, relación activa | Puerto de `student` | `403 STUDENT_NOT_MANAGED` |
| La clase existe | Puerto de `lesson` | `404 LESSON_NOT_FOUND` |
| La clase no está cancelada | La vista bloqueada | `409 LESSON_NOT_BOOKABLE` |
| La clase no ha empezado | La vista bloqueada y el reloj | `422 LESSON_ALREADY_STARTED` |
| Quedan plazas | Recuento bajo el cerrojo | `409 LESSON_FULL` |
| Sin reserva activa en esa clase | Comprobación previa + índice único | `409 BOOKING_ALREADY_EXISTS` |
| Sin otra reserva que solape | Comprobación previa + `EXCLUDE` | `409 STUDENT_SCHEDULE_OVERLAP` |

Dos notas sobre la tabla:

- **Un access token vive varios minutos** después de desactivar la cuenta. La comprobación de
  "gestionado y activo" es la que cierra ese hueco para reservar, porque se hace contra la base en
  cada petición, no contra el token.
- **Cancelada es `409`, empezada es `422`**, por el criterio de `11-contrato-api.md`: la pantalla
  que muestra una clase cancelada está obsoleta y releer la arregla; una clase empezada no deja de
  estarlo por releer.

`STUDENT_NOT_MANAGED` necesita un puerto nuevo en `student`, porque ninguno de los actuales
responde "¿gestiona este profesor a este alumno, y está activo?" sin cargar datos personales que
`booking` no necesita. Debe devolver un booleano, no una vista.

### `bookedCount` y `FULL` vuelven al contrato

Con `LessonBookings` implementado, `LessonView` recupera `bookedCount` y `statusAt` puede derivar
`FULL`. Dos cuidados:

- **El recuento es por lotes.** `GET /teacher/lessons` devuelve hasta 62 días de clases; contar
  una a una sería una consulta por clase. La operación recibe un conjunto de ids y devuelve un
  mapa, y resuelve todo con un `GROUP BY`.
- **El orden de precedencia del estado efectivo** queda: `CANCELLED` > `COMPLETED` > `FULL` >
  `OPEN`. Una clase llena que ya terminó es `COMPLETED`, no `FULL`: lo que el lector quiere saber
  de una clase pasada es que pasó.

### La cascada al cancelar una clase

Todas las reservas `CONFIRMED` de la clase pasan a `CANCELLED_BY_TEACHER` con el mismo
`cancelled_at` que la clase, en la misma transacción. Las ya canceladas no se tocan: su causa ya
está escrita y no es ésta.

### La cascada al desactivar un alumno

Se cancelan sus reservas `CONFIRMED` **de clases que todavía no han empezado**, con estado
`CANCELLED_BY_TEACHER` —fue el profesor quien lo desactivó— y sin ventana de 24 horas.

Las de clases pasadas no se tocan. Esas clases ocurrieron, y su asistencia —sea cual sea— es
histórico que `01-analisis-funcional.md` §5 pide conservar.

**Sólo se cancelan las reservas de las clases de ese profesor.** Con un profesor es lo mismo que
"todas"; con dos, desactivar un alumno en un sitio no puede borrarle las clases del otro. Es el
mismo razonamiento que ha llevado a comprobar propiedad en las fases 7 y 8 aunque hoy parezca
redundante.

### Cancelar una reserva

| Quién | Qué reservas | Ventana de 24 h | Estado resultante |
|---|---|---|---|
| Alumno | Las suyas | **Sí** — fuera de plazo, `422 CANCELLATION_WINDOW_EXPIRED` | `CANCELLED_BY_STUDENT` |
| Profesor | Las de sus clases | No | `CANCELLED_BY_TEACHER` |
| Admin | Cualquiera | No | `CANCELLED_BY_ADMIN` |

La ventana del alumno es la regla que la Fase 8 dejó en pie a propósito: protege al profesor de
un hueco que ya no puede llenar. El profesor no está atado por la misma razón que no lo está al
cancelar la clase entera, y sería incoherente que pudiera cancelar la clase y no una reserva
suya.

Una reserva de otro alumno responde **`404`**, no `403`, como en la Fase 8 con la clase de otro
profesor: confirmar que un id existe es decir algo que el llamante no tenía por qué saber.

Una reserva ya cancelada responde `409 BOOKING_ALREADY_CANCELLED`. Una reserva de una clase que ya
empezó no se puede cancelar: `422 LESSON_ALREADY_STARTED`, por la misma razón que la Fase 8 no
deja cancelar una clase terminada. A partir del inicio lo que toca es marcar asistencia.

### Marcar asistencia

- Sólo el profesor de la clase, sólo sobre reservas `CONFIRMED` de esa clase.
- **Sólo a partir del inicio de la clase**, como dice `01-analisis-funcional.md` §11. Antes,
  `422 ATTENDANCE_NOT_YET_OPEN`.
- **Se puede corregir**: marcar `NO_SHOW` a quien había marcado `ATTENDED` es un error de dedo
  que tiene que poder deshacerse. No hay tope temporal.
- La petición es **todo o nada**: si una de las entradas no pertenece a la clase o no está
  confirmada, no se aplica ninguna. Una asistencia a medias es peor que ninguna, porque parece
  completa.

### Listar reservas

`GET /bookings` ya está en el contrato, paginado. Propongo:

- **Alumno**: sus reservas, más recientes primero, filtrables por `status`.
- **Profesor**: las de sus clases, y además **filtrables por `lessonId`**. Sin ese filtro no hay
  forma de construir la pantalla de asistencia, que necesita justo los `bookingId` de una clase.
  El contrato no lo tiene y hay que añadirlo.

Cada reserva lleva su `lesson` incrustada, como ya dice el schema: la pantalla de "mis reservas"
necesita la hora de la clase y no debería pedirla una por una.

## Decisiones cerradas antes de implementar

Las siete se validaron tal como estaban propuestas. Se recogen con su respuesta para que el
documento se lea como lo que es: lo acordado.

1. **Cómo van las operaciones contra el grafo.** Acordado: **opción C**, inversión de
   dependencias, con un paquete nuevo por módulo para las interfaces que otro implementa y la
   regla de ArchUnit ampliada con nombre propio. Alternativas: A (`booking` orquesta) o B
   (eventos).
2. **Asistencia en columna propia**, corrigiendo `10-diagrama-er.md`, `01-analisis-funcional.md` y
   `openapi.yaml`. Acordado: sí.
3. **Sin trigger de capacidad.** Acordado: sin trigger; el cerrojo y el test de concurrencia
   bastan.
4. **¿Se puede volver a reservar una clase tras cancelar la reserva?** Es una de las pendientes de
   `01-analisis-funcional.md` §17. Acordado: **sí**, siempre que queden plazas y se cumpla todo lo
   demás. El índice único parcial ya lo permite, y como el alumno sólo puede cancelar con más de
   24 horas de antelación, no abre ningún abuso.
5. **¿Hasta cuándo se puede reservar?** Acordado: **hasta el inicio de la clase**. La
   consecuencia hay que aceptarla con los ojos abiertos: quien reserve con menos de 24 horas ya no
   podrá cancelar. Es coherente —la ventana protege al profesor de huecos que no puede llenar, y
   una reserva tardía llena uno— pero el frontend tiene que avisarlo antes de confirmar.
6. **Crear clases en el pasado**, que la Fase 8 dejó sin decidir para esta fase. Con la asistencia
   ya definida la pregunta tiene respuesta: una clase que se crea ya empezada **no puede tener
   reservas**, porque reservar exige que no haya empezado, así que tampoco puede tener
   asistencia. No sirve para nada. Acordado: **prohibirlo** con `422 LESSON_IN_THE_PAST` en
   `lesson`. Es un cambio en un módulo ya cerrado, y por eso se preguntó en vez de asumirlo.
7. **El admin en `POST /bookings/{id}/cancel`.** El contrato ya lo incluye y el coste es una rama
   en la autorización. Acordado: **incluirlo**, porque es la única salida documentada para
   resolver incidencias (`01-analisis-funcional.md`, decisiones resueltas) y dejarlo fuera
   obligaría a volver sobre el endpoint en la fase de `administration`.

## Códigos de error

Mismo criterio de `11-contrato-api.md`: 409 si releer puede cambiar la respuesta, 422 si es una
regla que releer no cambia.

| Código | HTTP | Endpoint | Estado |
|---|---|---|---|
| `LESSON_FULL` | 409 | `POST /lessons/{id}/bookings` | Ya en el contrato |
| `BOOKING_ALREADY_EXISTS` | 409 | `POST /lessons/{id}/bookings` | Ya en el contrato |
| `STUDENT_SCHEDULE_OVERLAP` | 409 | `POST /lessons/{id}/bookings` | Ya en el contrato |
| `STUDENT_NOT_MANAGED` | 403 | `POST /lessons/{id}/bookings` | Ya existe |
| `CANCELLATION_WINDOW_EXPIRED` | 422 | `POST /bookings/{id}/cancel` | Ya en el contrato |
| `LESSON_NOT_BOOKABLE` | 409 | `POST /lessons/{id}/bookings` | **Nuevo**: la clase está cancelada |
| `LESSON_ALREADY_STARTED` | 422 | reservar y cancelar reserva | **Nuevo** |
| `BOOKING_NOT_FOUND` | 404 | `/bookings/{id}/...` | **Nuevo** |
| `BOOKING_ALREADY_CANCELLED` | 409 | `POST /bookings/{id}/cancel` | **Nuevo** |
| `ATTENDANCE_NOT_YET_OPEN` | 422 | `POST /teacher/lessons/{id}/attendance` | **Nuevo** |
| `LESSON_IN_THE_PAST` | 422 | `POST /teacher/lessons` | **Nuevo**, decisión 6 |

## Criterios de aceptación

- **Dos reservas simultáneas de la última plaza: gana exactamente una, la otra recibe `409
  LESSON_FULL`, ninguna recibe un 500, y en la base queda una sola reserva.** El test usa un
  `CountDownLatch` para soltar las dos peticiones a la vez, y se repite varias veces en la misma
  ejecución: una carrera que se gana una vez no demuestra nada.
- El mismo alumno envía dos reservas simultáneas de la misma clase: una gana, la otra recibe `409
  BOOKING_ALREADY_EXISTS`.
- Un alumno reserva una clase con plaza y recibe `201`; la clase pasa a mostrar `bookedCount` uno
  más.
- Al ocupar la última plaza, la clase se lee `FULL`; al cancelar una reserva, vuelve a `OPEN`.
- Un alumno no gestionado recibe `403 STUDENT_NOT_MANAGED`; uno desactivado, también.
- Reservar una clase cancelada, empezada o llena devuelve su código.
- Una reserva que solapa con otra del mismo alumno se rechaza, **y un test que va directo al
  repositorio demuestra que la restricción existe** sin la comprobación previa del servicio.
- El alumno cancela con más de 24 horas y recibe `200`; con menos, `422`. El profesor cancela
  con menos de 24 horas y recibe `200`.
- Tras cancelar, el alumno puede volver a reservar la misma clase (decisión 4).
- Cancelar una clase deja todas sus reservas confirmadas en `CANCELLED_BY_TEACHER`, y si la
  cascada falla, la clase **no** queda cancelada.
- Desactivar un alumno cancela sus reservas futuras y respeta las pasadas.
- Marcar asistencia antes del inicio da `422`; después, se aplica y se puede corregir. Una
  entrada ajena en el lote hace que no se aplique ninguna.
- Un alumno que pide la reserva de otro recibe `404`.

## Estrategia de testing

- **Dominio, sin Spring**: transiciones de la reserva, ventana de 24 horas con los límites
  exactos (24 h justas, un segundo menos), estado efectivo de la clase con el recuento, y el
  orden `CANCELLED` > `COMPLETED` > `FULL` > `OPEN`.
- **Aplicación, con puertos simulados**: el orden de las comprobaciones —en particular, que el
  recuento se hace **después** de bloquear—, y que la cascada corre dentro de la transacción del
  llamante.
- **Integración con PostgreSQL real**: el índice único, la restricción de exclusión, la
  traducción de ambas por SQLState, y que el cerrojo de `lesson` exige transacción.
- **Concurrencia**: el test del criterio de salida. Es el único de la suite que necesita hilos, y
  el pool de conexiones del perfil de test tiene que tener al menos dos, o el test se
  serializará solo y pasará sin probar nada. Hay que comprobarlo, no suponerlo.
- **Arquitectura**: la ampliación de `ModuleBoundariesTest` tiene su propio test que demuestra
  que un módulo puede *implementar* una interfaz provista por otro y **no** llamarla.

## Lo que hay que tocar y suele olvidarse

- `AbstractIntegrationTest`: `bookings` en la lista del `TRUNCATE`.
- `ModuleBoundariesTest`: la arista `booking → identity` (por el adaptador web, como en las
  fases 7 y 8) y la regla nueva de interfaces provistas.
- `LessonEntity`: el comentario que anuncia `@Version` para esta fase.
- `ManageStudent.stopManaging` y `ManagedStudent`: el javadoc que dice que la cascada falta.
- `openapi.yaml`: `bookedCount` vuelve, `BookingStatus` pierde dos valores, `Booking` gana
  `attendance`, `GET /bookings` gana `lessonId`, los códigos nuevos, y los `description` que
  dicen "pendiente hasta la Fase 9".
- `11-contrato-api.md`: la tabla de códigos.
- `10-diagrama-er.md`: el changeset de `bookings` con la asistencia separada, y sin el trigger.
- `01-analisis-funcional.md` §10: todavía dice que la ventana de 24 horas ata al profesor. La
  Fase 8 corrigió `01-analisis-funcional.md` y se dejó éste.
- `CLAUDE.md`: la tabla de fases y las trampas nuevas.
- Cada error nuevo se mapea con `Problems.of`, y los tests de API heredan de
  `AbstractIntegrationTest`.

## Documentos que esta fase corrige

- **`10-diagrama-er.md`**: la asistencia sale de `bookings.status` a su propia columna, y el
  trigger opcional se descarta.
- **`01-analisis-funcional.md`**: los estados de la reserva, por lo mismo.
- **`01-analisis-funcional.md`**: la ventana de 24 horas en §10, y las dos pendientes de §17 que
  esta fase cierra (volver a reservar, y el admin y la ventana).
- **`03-modelo-de-datos.md`**: el solapamiento del alumno pasa de cerrojo sobre el alumno a
  restricción de exclusión.
- **`02-arquitectura.md` §4**: `booking` depende también de `identity`, y el grafo gana el
  mecanismo de interfaces provistas.
- **`openapi.yaml`** y **`11-contrato-api.md`**: lo listado arriba.

## Entrega

Un solo PR desde `fase-9-booking`, con este análisis primero —validado antes de escribir código—
y la implementación después, como en las fases 7 y 8.

## Decisiones tomadas al implementar

Lo que no se podía saber hasta escribir el código. Va aquí y no sólo en el informe de cierre,
porque es la clase de detalle que la siguiente fase necesita y un informe fechado no se relee.

### El email verificado sólo lo puede exigir `booking`: `403 EMAIL_NOT_VERIFIED`

La tabla de condiciones de este análisis daba por hecho que "verificado" venía garantizado por el
token. No es así: el login acepta una cuenta sin verificar —`openapi.yaml` lo dice expresamente—,
y rellenar el perfil y ser gestionado tampoco lo exigen. Reservar es, por tanto, el único sitio
donde la regla de `01-analisis-funcional.md` §9 puede cumplirse. Se usa el `emailVerified` del
token, que el propio javadoc de `AuthenticatedUser` ya declaraba "aceptable para reservar": como
mucho va un tiempo de vida del token por detrás de la realidad, y en esa dirección —alguien que
acaba de verificar y aún no puede reservar— el coste es volver a iniciar sesión.

### El profesor se copia en la reserva

Además de los instantes de la clase, `bookings` guarda `teacher_user_id`. Todas las consultas
del lado del profesor —listar, cancelar, asistencia, la cascada al desactivar un alumno— filtran
por él, y la alternativa era un `JOIN` contra `lessons`, que es la tabla de otro módulo. La copia
es segura por la misma razón que la de los instantes: una clase nunca cambia de profesor.

### El paquete se llama `application/port/spi`, y la regla sólo deja implementarlo

La opción C necesitaba un sitio para las interfaces que un módulo declara y otro implementa. Se
llama `spi` —*service provider interface*, el nombre establecido para exactamente esto— y
`ModuleBoundariesTest` acepta una dependencia sobre él **sólo desde una clase que implementa esa
interfaz**. Una clase de `booking` que la inyectara para llamarla sería rechazada, y hay un test,
`PublicPortsTest`, que lo demuestra con módulos de prueba: una regla que sólo se ha visto pasar no
prueba nada sobre lo que debe impedir.

### El cerrojo lo presta `lesson`, y cuenta él mismo después de cerrarlo

`LockLesson.lockForBooking` hace el `SELECT ... FOR UPDATE` y, ya con la fila bloqueada, pide el
recuento a `LessonBookings`. Así el `FULL` que devuelve está calculado bajo el cerrojo y por la
única regla que existe —`Lesson.statusAt`—, y `booking` no repite la comparación entre plazas y
capacidad. Exige transacción (`Propagation.MANDATORY`): llamado fuera de una, el cerrojo se
soltaría al volver y no protegería nada, y es mejor que eso falle con una excepción que con una
carrera.

Por la misma razón, la respuesta de una reserva recién hecha **vuelve a leer la clase** en vez de
sumar uno a mano: sumar a mano habría sido una segunda copia de la regla de `FULL`.

### El test de concurrencia se comprobó rompiendo lo que prueba

`LastSeatConcurrencyTest` lanza dos reservas de la última plaza soltadas por el mismo
`CountDownLatch`, cinco veces seguidas. Pasa. Para saber si eso significa algo, se quitó el
`@Lock` de `LessonJpaRepository.findByIdForUpdate` y se volvió a ejecutar: **falla**, con las dos
reservas dentro. El cerrojo es lo que gana la carrera, no la suerte. El mismo test comprueba que
el pool de conexiones admite al menos dos, porque con una sola las peticiones harían cola en el
pool en vez de en el cerrojo y el test pasaría sin probar nada.

### Reutilizar códigos antes que inventarlos

- **`AUTH_FORBIDDEN`** para "este rol no hace esto": un profesor que intenta reservar, un admin
  que pide "mis reservas". Es exactamente lo que ese código ya significaba en la cadena de
  seguridad.
- **`VALIDATION_ERROR`** para un filtro `status` desconocido o una reserva repetida en un lote de
  asistencia. Con un detalle que habría fallado en silencio: si el parámetro se declara con el
  enum de dominio, el conversor de Spring lanza una excepción que ningún advice captura y responde
  un **500**. Por eso el filtro llega como texto y lo interpreta `BookingStatus.filter`.

### Filtros opcionales con `Specification`, no con `:param is null`

El listado tiene hasta tres filtros opcionales. La forma corta en JPQL —`(:lessonId is null or
b.lessonId = :lessonId)`— falla en PostgreSQL con un UUID nulo, porque la base no puede deducir el
tipo del parámetro. Se construye con `JpaSpecificationExecutor`, que simplemente no añade la
condición cuando el filtro no viene.

### La clase que viaja dentro de una reserva no lleva `notes`

Para nadie. Quién ve las notas lo decide `lesson` al servir la clase; repetir aquí la regla del
dueño habría sido una segunda copia de ella, y ninguna pantalla de reservas las necesita.

### No se extrajo una página genérica a `shared`

El javadoc de `ManagedStudentPage` anunciaba que, al llegar el segundo listado, habría que llevar
un tipo de página común a `shared`. Llegó y no se hizo: exigiría que **todos** los módulos
pudieran depender de `shared`, un cambio en el grafo entero, para ahorrarse un record de cuatro
campos. `BookingPage` es el segundo; si llega un tercero con la misma forma —la lista de usuarios
de `administration`— vuelve a valorarse.

### Una clase en curso se puede cancelar, y la asistencia marcada se conserva

La Fase 8 sólo impide cancelar una clase que ya **terminó**. Una en curso se puede cancelar, y la
cascada pasa sus reservas a `CANCELLED_BY_TEACHER` aunque alguna tenga ya la asistencia marcada.
Por eso el esquema **no** tiene un `CHECK` que obligue a que sólo las reservas confirmadas tengan
asistencia: ese `CHECK` habría convertido esa cancelación en un 500. La asistencia marcada queda
como histórico de lo que pasó antes de cancelar.

### Lo que queda sin resolver

- **`updated_at` no se actualiza** ni en `lessons` ni en `bookings`: las dos columnas tienen
  `DEFAULT now()` y ninguna entidad las mapea, así que conservan la fecha de creación para
  siempre. Hoy nadie las lee. Si alguna vez se necesitan, o se mapean con
  `@UpdateTimestamp` o se quitan; mantenerlas mintiendo es lo único que no vale.
- **La modificación de clases** sigue fuera. Cuando llegue, tendrá que actualizar en la misma
  transacción las copias de instantes que guardan las reservas; está escrito en el changeset
  `v7-booking` para que no sea una sorpresa.
