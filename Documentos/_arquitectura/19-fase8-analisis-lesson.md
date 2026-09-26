# Fase 8 — Análisis: las clases del profesor (`lesson`)

Análisis previo a escribir código, como las fases 6 y 7. Es la Fase 4 de
`12-metodologia-trabajo.md`, que numera distinto que la tabla de `12-metodologia-trabajo.md`.

`lesson` es la primera consumidora de `QueryAvailability`, el puerto que la Fase 7 dejó
preparado y que hasta hoy no tiene ningún llamador.

## Alcance

El criterio de salida del roadmap es: **el profesor puede crear clases individuales y grupales
respetando —o forzando explícitamente— su disponibilidad**.

Entra:

- Crear una clase, individual o grupal, con validación de duración, de medianoche, de
  solapamiento con otras clases del profesor y contra la disponibilidad configurada.
- La acción explícita que permite crear fuera de disponibilidad, y que queda registrada.
- Consultar una clase y listar las clases de un rango.
- Cancelar una clase.
- El changeset de `lessons` y su restricción de exclusión.

No entra, y el apartado siguiente explica por qué no puede entrar: nada que dependa de saber
cuántas reservas tiene una clase.

## El problema de fondo: `lesson` no puede ver `booking`

Esta es la decisión que condiciona todas las demás, así que va antes que ellas.

El grafo de módulos dice `booking → lesson`: las reservas conocen las clases, y las clases no
conocen las reservas. La regla la aplica `ModuleBoundariesTest` y no es negociable sin rehacer el
grafo. Pero el contrato que `openapi.yaml` ya tiene escrito para esta fase pide tres cosas que
**solo se pueden responder contando reservas**:

- `Lesson.bookedCount`, el número de plazas ocupadas.
- El estado `FULL`, que por definición es "no quedan plazas".
- `POST /teacher/lessons/{id}/attendance`, cuyo cuerpo son `bookingId`s y cuya respuesta es una
  lista de `Booking`.

Y una cuarta, en el `summary` de `POST /teacher/lessons/{id}/cancel`: «cancela en cascada sus
reservas activas».

Ninguna de las cuatro la puede implementar `lesson`. No es un problema de esta fase: es que el
contrato se escribió de una vez, antes de que existiera el grafo por el que ahora pasa.

### Lo que se decidió

**Asistencia y cascada de cancelación se aplazan a la Fase 9 (`booking`)**, que es el módulo que
puede escribirlas. Sus rutas siguen colgando de `/teacher/lessons/{id}/...` —una ruta no implica
qué módulo la sirve— pero el adaptador que las atienda vivirá en `booking`.

**`bookedCount` se retira del contrato hasta la Fase 9**, en lugar de devolver `0` o `null`. Hay
precedente explícito y reciente: `RegisterRequest` retiró `fullName` hasta la Fase 6 con esta
razón escrita en el propio spec —«aceptar el campo y descartarlo haría creer al cliente que se ha
guardado»—. Un `bookedCount: 0` constante es peor que la ausencia del campo, porque parece un
dato.

**`FULL` y `COMPLETED` dejan de ser estados almacenados y pasan a derivarse en lectura.** Esto es
una corrección a `10-diagrama-er.md`, y la justifico aparte porque es la parte discutible.

### Por qué `FULL` no puede ser una columna

Si `lessons.status` puede valer `FULL`, alguien tiene que escribirlo cuando entra la última
reserva y devolverlo a `OPEN` cuando una se cancela. Ese alguien solo puede ser `booking`, y
entonces `booking` estaría escribiendo el estado de una clase: exactamente lo que el grafo
permite —a través de un puerto de entrada de `lesson`— pero que deja el dato duplicado en dos
sitios que se pueden contradecir. El día que se contradigan, la clase dirá `OPEN` con las plazas
agotadas, o `FULL` con una plaza libre, y ninguna de las dos cosas falla: simplemente miente.

Es el mismo razonamiento que llevó en la Fase 7 a que la disponibilidad se resolviera en un solo
sitio en vez de copiarse.

La alternativa, que es la acordada: **la columna guarda solo lo que decide una persona** —`OPEN` o
`CANCELLED`— y la API expone un estado efectivo que se calcula al leer:

- `CANCELLED` si la columna lo dice.
- `COMPLETED` si la clase ya terminó. Se deriva del reloj y no necesita ningún proceso
  programado, que el MVP no tiene.
- `FULL` cuando las plazas ocupadas igualan la capacidad. **Este solo lo podrá calcular la Fase
  9**, porque necesita el recuento; hasta entonces una clase abierta se lee `OPEN`.
- `OPEN` en el resto de casos.

La restricción `EXCLUDE ... WHERE (status <> 'CANCELLED')` sigue funcionando igual, porque solo
le importa si está cancelada o no.

El coste de esta decisión es que el `CHECK (status IN ('OPEN','FULL','CANCELLED','COMPLETED'))`
del esquema previsto se queda en dos valores, y que un informe que quiera contar clases
completadas tiene que filtrar por fecha en vez de por estado. Me parece un precio bajo frente a
tener dos fuentes para el mismo hecho.

La alternativa —estado almacenado y un puerto de entrada que `booking` llame— se consideró y se
descartó. Era defendible, y habría cambiado poco de esta fase y bastante de la Fase 9; se eligió
antes de escribir el changeset precisamente porque después habría costado una migración.

## Decisiones

### La duración se mide en tiempo real, no en hora de pared

`starts_at` y `ends_at` son instantes. La regla «mínimo 30 minutos y múltiplo de 30» se aplica a
la diferencia entre esos dos instantes, que es lo que el `CHECK` de la base puede comprobar.

Esto tiene una consecuencia que hay que aceptar con los ojos abiertos: **los dos días del año en
que cambia la hora, la duración real y la duración aparente en el reloj del profesor no
coinciden**. Una clase que en su reloj va de 01:30 a 03:30 el día que se adelanta la hora dura
una hora de verdad, no dos. Y la hora local 02:30 de ese día sencillamente no existe.

La decisión es que **manda el instante**: la clase dura lo que dura, y la validación no intenta
adivinar qué quiso decir el profesor. El frontend, que es quien traduce el reloj a instantes, es
el que tiene que enseñar la duración resultante antes de confirmar.

La regla **«no cruza medianoche» sí se evalúa en hora de pared**, con la zona del profesor,
porque medianoche es un concepto local: una clase de 23:00 a 00:30 del día siguiente se rechaza
aunque en UTC no tenga nada de particular. Como en la Fase 7, **las fechas de cambio de hora se
escriben literales en los tests**: un test que le pregunta a `java.time` cuándo cambia la hora se
da la razón a sí mismo.

### El solapamiento se comprueba dos veces, y las dos hacen falta

La restricción `EXCLUDE USING gist` garantiza que no haya dos clases del profesor pisándose. Pero
una violación de restricción llega como una excepción de la base que, sin traducir, sale como un
500.

Se comprueba antes en la aplicación para poder responder `409 LESSON_OVERLAP` con un mensaje
útil, y **se traduce además la violación de la restricción al mismo 409**, porque entre la
comprobación y el `INSERT` cabe otra petición. La comprobación previa es para el mensaje; la
restricción es la que garantiza. Quitar cualquiera de las dos deja un agujero: sin la primera,
respuestas incomprensibles; sin la segunda, una carrera que el MVP sí puede correr, porque el
profesor puede tener dos pestañas abiertas.

La restricción necesita la extensión `btree_gist`, que **todavía no está instalada**: hay que
crearla en el changeset de esta fase.

### La disponibilidad se consulta, no se reimplementa

La comprobación es una llamada a `QueryAvailability.covers(teacherUserId, startsAt, endsAt)`, el
puerto que la Fase 7 dejó hecho justamente para esto. `lesson` no lee reglas ni excepciones.

Si no cubre y el profesor no ha pedido forzarlo, se rechaza. Si ha pedido forzarlo, se crea y la
fila **queda marcada** con `created_outside_availability`, porque una clase creada a propósito
fuera del horario y una creada por error se distinguen luego solo si se anotó en su momento.

Aquí aparece la deuda que la Fase 7 dejó anotada: **`QueryAvailability` no acota el rango que
acepta**, al contrario que `GetAvailability`, que lo limita a 62 días. En esta fase el rango lo
fija la propia clase, que dura horas, así que el problema no se manifiesta. La propuesta es
**dejarlo así y anotarlo en el puerto**, porque acotar por acotar un método cuyo único llamador
pasa un rango de horas es resolver un problema que nadie tiene. Vuelve a mirarse en `calendar`,
que sí pedirá rangos largos.

### Quién puede leer una clase

Misma lógica que la disponibilidad en la Fase 7: **cualquier usuario autenticado puede leer una
clase**, porque un alumno tiene que poder ver a qué se apunta, y la clase no contiene datos
personales de nadie.

Con una excepción: **`notes` es del profesor**. Es un campo de texto libre donde cabe «insistir
con el revés» o cualquier otra cosa que no se escribió para que la lea el alumno. Propongo que
`notes` solo viaje en las respuestas cuando quien pregunta es el profesor. Es más fácil decidirlo
ahora que quitarlo después de que un frontend lo esté pintando.

### Escribir solo el profesor, y comprobando propiedad

Como en la Fase 7: rol **y** pertenencia, no solo el rol del token. El `teacherUserId` sale
siempre del token autenticado y nunca del cuerpo de la petición.

Con un único profesor la comprobación de propiedad parece redundante. No lo es: es la misma razón
por la que la Fase 7 dejó de caer en la zona del único profesor —el día que haya dos, una
comprobación que falta no falla, acierta con la clase equivocada.

### Cancelar una clase: la ventana de 24 horas no debería atar al profesor

Aquí hay una regla del producto que **creo que está mal** y prefiero señalarla antes de
implementarla.

`01-analisis-funcional.md` dice «las cancelaciones se permiten hasta 24 horas antes» y «el ADMIN
puede cancelar clases o reservas sin respetar la ventana». `openapi.yaml` lo traduce a un `422
CANCELLATION_WINDOW_EXPIRED` en `/teacher/lessons/{id}/cancel`.

Leído tal cual: **si el profesor se pone enfermo la noche antes, no puede cancelar su propia
clase.** La única salida sería que un ADMIN lo hiciera por él, y en este MVP el ADMIN no es una
persona de guardia: es una cuenta.

La ventana de 24 horas tiene sentido para el alumno, porque protege al profesor de un hueco que
ya no puede llenar. Aplicada al profesor sobre su propia clase no protege a nadie: deja al
sistema sin la operación que la realidad va a pedir, y el resultado práctico será una clase
fantasma en el calendario a la que no va nadie.

**Decidido: se levanta para el profesor.** El profesor puede cancelar su clase en cualquier
momento, y la cancelación dentro de la ventana **queda registrada como tal** —`cancelled_at` más
el hecho de si se hizo con menos de 24 horas— para que la Fase 9 pueda avisar a los alumnos
afectados sin tener que recalcularlo después.

La ventana de 24 horas se mantiene **intacta para el alumno que cancela su reserva**, que es
donde el producto la justificó: protege al profesor de un hueco que ya no puede llenar.

`01-analisis-funcional.md` hay que corregirlo, porque hoy dice lo contrario.

### Qué pasa con una clase cancelada

No se borra. Cambia de estado y deja de contar para el solapamiento, que es lo que la cláusula
`WHERE (status <> 'CANCELLED')` de la restricción ya hace. Eso permite crear otra clase en el
mismo hueco, que es justo lo que el profesor va a querer hacer después de cancelar.

Una clase cancelada **no se puede volver a abrir**: reabrir una clase cuyas reservas se
cancelaron en cascada dejaría a los alumnos fuera sin decírselo. Si el profesor se arrepiente,
crea otra.

### Capacidad de una clase grupal: hace falta un tope

El esquema dice `capacity > 0` y el contrato `minimum: 1`. No hay máximo. Nada impide hoy crear
una clase grupal de 10.000 plazas, y la única pista es una pista.

**Decidido**: existe un tope, es **configurable con 8 por defecto**, y vive en
`platform_configuration` —que ya es la dueña de la configuración global y ya tiene el límite de
alumnos— en vez de ser una constante en el código.

Ocho es un número razonable para una pista de tenis, pero lo que importa es que se pueda cambiar
sin desplegar: el día que el número esté mal, estará mal para todas las clases a la vez.

Esto tiene una consecuencia en el grafo: `lesson` necesita leer `platform`, igual que `student`
lee de ahí el límite de alumnos. `platform` no depende de nada por diseño, precisamente para que
cualquiera pueda leerlo sin crear un ciclo, así que la arista es legítima — pero hay que añadirla
a `ModuleBoundariesTest` junto con `lesson → identity`.

## Decisiones cerradas antes de implementar

Las cinco preguntas que este análisis dejó abiertas están respondidas. Se recogen aquí con su
respuesta para que el documento se lea como lo que es: lo acordado, no lo propuesto.

1. **La ventana de 24 horas no ata al profesor.** Se levanta para el profesor sobre su propia
   clase y se mantiene para el alumno sobre su reserva.
2. **El tope de capacidad de grupo es configurable, con 8 por defecto**, en
   `platform_configuration`.
3. **`FULL` y `COMPLETED` se derivan al leer.** La columna guarda solo `OPEN` y `CANCELLED`.
4. **Se añade `GET /teacher/lessons`**, con rango de fechas obligatorio y el mismo tope de 62
   días que usa la disponibilidad. Sin él, el profesor no puede ver lo que ha creado hasta que
   exista el calendario de la Fase 10.
5. **La modificación de clases queda fuera de esta fase.** Cambiar la hora de una clase que ya
   tiene reservas es una operación que afecta a `booking`; escribirla antes de que `booking`
   exista significa escribirla dos veces. Cancelar y volver a crear cubre el caso mientras tanto,
   y `12-metodologia-trabajo.md` se corrige para que deje de prometerla aquí.

## Códigos de error nuevos

Siguiendo el criterio de `11-contrato-api.md`: 409 cuando el cliente tiene un estado obsoleto y
releer puede cambiar la respuesta, 422 cuando es una regla de negocio y releer no cambia nada.

| Código | HTTP | Endpoint | Motivo |
|---|---|---|---|
| `LESSON_OVERLAP` | 409 | `POST /teacher/lessons` | Ya contratado. Choca con otra clase no cancelada del profesor |
| `LESSON_OUTSIDE_AVAILABILITY` | 422 | `POST /teacher/lessons` | Fuera del horario y sin pedir forzarlo. Releer no lo cambia: o se cambia la hora o se fuerza |
| `LESSON_INVALID` | 400 | `POST /teacher/lessons` | Duración que no es múltiplo de 30, menor de 30, cruza medianoche, o fin antes que inicio |
| `LESSON_NOT_FOUND` | 404 | `/lessons/{id}` | No existe esa clase |
| `LESSON_ALREADY_CANCELLED` | 409 | `POST /teacher/lessons/{id}/cancel` | Ya estaba cancelada; casi siempre una pantalla obsoleta |

`TEACHER_FORBIDDEN` ya existe y se reutiliza para las escrituras.

## Criterios de aceptación

- El profesor crea una clase individual dentro de su disponibilidad y recibe `201`.
- El profesor crea una clase grupal con capacidad mayor que uno.
- Una clase individual con capacidad distinta de uno se rechaza.
- Una duración de 20 minutos, de 45, o negativa, se rechazan.
- Una clase de 23:00 a 00:30 en la zona del profesor se rechaza por cruzar medianoche, **y la
  misma franja en una zona distinta no se rechaza si allí no cruza**.
- Una clase que solapa con otra existente se rechaza con `409`, y una que solapa con una
  **cancelada** se acepta.
- Una clase fuera de la disponibilidad se rechaza con `422`; con la acción explícita se crea y la
  fila queda marcada.
- Un alumno autenticado lee una clase y **no ve `notes`**; el profesor sí.
- Un alumno que intenta crear una clase recibe `403`.
- El profesor cancela una clase y deja de contar para el solapamiento.
- Cancelar dos veces la misma clase responde `409`.
- Una clase cuya hora de fin ya pasó se lee como `COMPLETED` sin que nadie haya ejecutado nada.

## Estrategia de testing

Igual que en las dos fases anteriores, y por las mismas razones:

- **Dominio, sin Spring**: duración, medianoche en varias zonas, capacidad según el tipo, y la
  derivación del estado efectivo. Son los tests que se ejecutan en milisegundos y donde de verdad
  se pilla un error de reglas.
- **Aplicación, con puertos simulados**: la decisión de forzar o no forzar la disponibilidad, y
  que `QueryAvailability` se llama con los instantes correctos.
- **Integración con PostgreSQL real**: la restricción de exclusión —que es la única prueba de que
  existe—, la traducción de su violación a `409`, y la extensión `btree_gist`.
- **Los dos días de cambio de hora, con las fechas escritas literales.**
- **La tabla `lessons` hay que añadirla al `TRUNCATE` de `AbstractIntegrationTest`.** Es la trampa
  documentada: una tabla que no está en esa lista conserva filas entre tests y el resultado
  vuelve a depender del orden.

## Lo que hay que tocar y suele olvidarse

- `AbstractIntegrationTest`: la lista del `TRUNCATE`.
- `ModuleBoundariesTest`: `lesson` va a necesitar `identity` por la misma razón que
  `availability` —un adaptador web tiene que saber quién llama—, así que la arista
  `lesson → identity` hay que añadirla a `lessonDependsOnTeacherAndAvailability`. Está
  anticipado en el javadoc de esa regla desde la Fase 7.
- `openapi.yaml`: los códigos nuevos, la retirada de `bookedCount`, el listado si se aprueba, y
  `notes` condicionado al rol.
- `11-contrato-api.md`: la tabla de códigos.
- `10-diagrama-er.md`: el `CHECK` de `status`, si se aprueba derivar `FULL` y `COMPLETED`.
- `CLAUDE.md`: la tabla de fases y las trampas nuevas.
- El error nuevo se mapea llamando a `Problems.of`, y el test de API hereda su fontanería de
  `AbstractIntegrationTest`. Las dos cosas están en `CLAUDE.md` desde la Fase 7.

## Documentos que esta fase corrige

- **`10-diagrama-er.md`**: `lessons.status` pasa de cuatro valores a dos.
- **`openapi.yaml`**: `bookedCount` se retira hasta la Fase 9; el `summary` de la cancelación
  promete una cascada que esta fase no puede hacer; `attendance` se sirve desde `booking`.
- **`12-metodologia-trabajo.md`**: promete «modificación» en esta fase; se quita.
- **`01-analisis-funcional.md`**: la ventana de 24 horas deja de aplicarse al profesor sobre su
  propia clase.
- **`10-diagrama-er.md`** otra vez: `platform_configuration` gana el tope de capacidad de grupo.

## Entrega

Un solo PR desde `fase-8-lesson`, con el análisis primero —este documento, validado antes de
escribir una línea— y la implementación después, como en la Fase 7.

Este documento se validó antes de escribir código, con las cinco decisiones del apartado
"Decisiones cerradas antes de implementar" acordadas explícitamente.


## Decisiones tomadas al implementar

Lo que no se podía saber hasta escribir el código. Va aquí y no sólo en el informe de cierre,
porque es la clase de detalle que la siguiente fase necesita y un informe fechado no se relee.

### Un código de error que el análisis no había previsto: `LESSON_ALREADY_FINISHED`

El análisis decidió qué pasa al cancelar dos veces, pero no qué pasa al cancelar una clase que
ya ocurrió. Sin una regla, se podía cancelar una clase del año pasado, que es reescribir el
pasado: los alumnos vinieron o no vinieron, y la Fase 9 registrará cuál de las dos. Se rechaza
con `422 LESSON_ALREADY_FINISHED`, un 422 y no un 409 porque releer no cambia nada — el tiempo
sólo va en una dirección.

### La marca de "fuera de disponibilidad" registra el hecho, no la intención

`created_outside_availability` se escribe a partir de si la clase **estaba** fuera del horario,
no de si el llamante pidió forzarlo. Una clase que cae dentro de las horas no es "fuera de
horario" porque quien la creó viniera preparado para que lo fuera. Como efecto secundario, sólo
hace falta una llamada a `QueryAvailability.covers` por petición en vez de dos.

### La clase de otro profesor responde 404, no 403

Con un único profesor el caso es inalcanzable. El día que no lo sea, decirle a alguien que un id
existe pero no es suyo es decirle algo que no tenía forma de saber. Es el mismo criterio que la
Fase 7 aplicó al borrar una excepción de disponibilidad ajena.

### `version` existe en la tabla y no se mapea

La columna está, con su `DEFAULT 0`, porque `10-diagrama-er.md` la preveía y añadirla después
sería otra migración. Pero la entidad JPA **no** la mapea: el bloqueo optimista que le da sentido
llega con la Fase 9, junto al `SELECT ... FOR UPDATE` que protege la última plaza, y mapear
`@Version` ahora sólo significaría arrastrar un número que nadie lee y que complica escribir una
entidad reconstruida desde el dominio.

Por eso mismo el adaptador **carga la fila y la modifica** en lugar de guardar una copia
separada: así las columnas que este módulo no mapea —`version`, `created_at`— conservan lo que
les dio la base.

### La violación de la restricción se traduce por el nombre de la restricción, y con `flush`

Dos detalles que no se ven hasta que fallan. El primero: `saveAndFlush` y no `save`, porque sin
el flush la violación aparece al confirmar la transacción, que es después de que el controlador
haya respondido — llegaría como un 500 imposible de mapear. El segundo: se compara el **nombre**
`ex_lessons_no_teacher_overlap` y no el texto del mensaje, porque una clase también puede
romper el `CHECK` de duración o la clave ajena, y ésos no son un solapamiento en absoluto.

### `platform` pasó a tener un solo servicio para sus dos límites

Añadir el tope de capacidad iba a duplicar el servicio existente entero: el fallback, el aviso
en el log y la lectura de la fila son idénticos, y sólo cambia qué campo se devuelve.
`GetStudentLimitService` se sustituyó por `PlatformLimitsService`, que implementa los dos
puertos. Los puertos siguen separados, de modo que `student` continúa dependiendo sólo del
límite que lee.

Al cablearlo se cometió —y se corrigió— un error que merece quedar escrito: registrarlo **a la
vez** como bean de su clase concreta y como bean de cada puerto crea tres beans del mismo objeto
y hace ambigua cualquier inyección. El contexto no arranca, con
`NoUniqueBeanDefinitionException`. Se declara un único bean por su tipo concreto, y la inyección
por cualquiera de las dos interfaces encuentra exactamente un candidato.

### `LessonDateRange` duplica `AvailabilityDateRange`, y era inevitable

Misma forma y mismo tope de 62 días. Las reglas de frontera impiden compartir un tipo de dominio
entre módulos, así que la duplicación es forzada, igual que la de
`TeacherRoleRequiredException` — que con esta fase va ya por la cuarta copia. Lo que sí se
comparte a propósito es el número: un profesor que pudiera leer dos meses de disponibilidad y
sólo uno de clases habría encontrado una diferencia que nadie decidió.

### La exclusión de SpotBugs no hizo falta aquí, y eso confirma la medición de la Fase 7

Los cuatro servicios de `lesson` guardan sus puertos exactamente igual que los de
`availability`, y **SpotBugs no marca ninguno**: `mvn verify` da `BugInstance size is 0` sin
tocar `spotbugs-exclude.xml`. Es la confirmación práctica de lo que la sonda de la Fase 7 había
medido — el detector se disparaba por aquellos dos tipos concretos, no por el patrón de
constructor — y la razón por la que ensanchar la exclusión al paquete habría sido taparse los
ojos.

### Lo que queda sin decidir

**Nada impide crear una clase en el pasado.** Se puede, forzando la disponibilidad, y nace
`COMPLETED`. No se ha prohibido porque ninguna regla del producto lo pide y el análisis no lo
planteó, así que rechazarlo habría sido inventar una regla. Puede ser útil —registrar una clase
que ya se dio— o puede ser un error de tecleo que nadie detiene. Merece una decisión explícita
en la Fase 9, cuando el marcado de asistencia le dé un sentido u otro.
