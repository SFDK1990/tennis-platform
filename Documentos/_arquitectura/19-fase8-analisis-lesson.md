# Fase 8 — Análisis: las clases del profesor (`lesson`)

Análisis previo a escribir código, como las fases 6 y 7. Es la Fase 4 de
`09-roadmap-implementacion.md`, que numera distinto que la tabla de `12-metodologia-trabajo.md`.

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

### Lo que propongo

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

La alternativa que propongo: **la columna guarda solo lo que decide una persona** —`OPEN` o
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

**Si prefieres lo contrario** —estado almacenado y un puerto de entrada que `booking` llame—, es
una decisión defendible y cambia poco de esta fase: cambia la Fase 9. Pero hay que elegir ahora,
porque el changeset de `lessons` se escribe en esta.

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

`01-product-architect.md` dice «las cancelaciones se permiten hasta 24 horas antes» y «el ADMIN
puede cancelar clases o reservas sin respetar la ventana». `openapi.yaml` lo traduce a un `422
CANCELLATION_WINDOW_EXPIRED` en `/teacher/lessons/{id}/cancel`.

Leído tal cual: **si el profesor se pone enfermo la noche antes, no puede cancelar su propia
clase.** La única salida sería que un ADMIN lo hiciera por él, y en este MVP el ADMIN no es una
persona de guardia: es una cuenta.

La ventana de 24 horas tiene sentido para el alumno, porque protege al profesor de un hueco que
ya no puede llenar. Aplicada al profesor sobre su propia clase no protege a nadie: deja al
sistema sin la operación que la realidad va a pedir, y el resultado práctico será una clase
fantasma en el calendario a la que no va nadie.

**Propuesta**: el profesor puede cancelar su clase en cualquier momento, y la cancelación dentro
de la ventana queda registrada como tal para que la Fase 9 pueda avisar a los alumnos afectados.
La ventana de 24 horas se mantiene intacta **para el alumno que cancela su reserva**, que es
donde el producto la justificó.

**Esto necesita tu validación explícita**, porque cambia una regla de negocio escrita, no un
detalle de implementación. Si decides mantenerla tal cual, se implementa tal cual y lo anoto como
riesgo operativo conocido.

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

No hay ningún número acordado en los documentos, así que **no me lo invento**: es una de las
preguntas abiertas de más abajo. Lo que sí propongo es que exista un tope, y que viva en
`platform_configuration` —que ya es la dueña de la configuración global y ya tiene el límite de
alumnos— en vez de ser una constante en el código.

## Lo que necesito que decidas

Son las tres cosas que no puedo resolver leyendo los documentos, porque no están en ellos o
porque contradicen algo escrito.

1. **La ventana de 24 horas aplicada al profesor** (apartado de cancelación). ¿Se levanta para el
   profesor sobre su propia clase, o se mantiene tal como está escrita?
2. **El tope de capacidad de una clase grupal.** ¿Qué número, y va en `platform_configuration` o
   se queda sin tope en el MVP?
3. **`FULL` derivado o almacenado.** Propongo derivado, por lo explicado arriba. Cambia poco esta
   fase y bastante la siguiente, así que conviene cerrarlo ahora.

Y dos que puedo decidir yo, pero que prefiero que veas porque amplían el contrato:

4. **Falta un listado de clases.** `openapi.yaml` tiene `POST /teacher/lessons` y
   `GET /lessons/{id}`, pero no hay forma de listar. El profesor no puede ver lo que ha creado
   sin guardar los identificadores, y el calendario que resolvería esto es la Fase 10. Propongo
   añadir `GET /teacher/lessons` con rango de fechas obligatorio y el mismo tope de 62 días que
   usa la disponibilidad, por coherencia.
5. **La modificación de clases.** El roadmap dice «creación, consulta, modificación y
   cancelación», pero el contrato no tiene ningún `PATCH`. Propongo **dejar la modificación
   fuera** de esta fase: cambiar la hora de una clase con reservas es una operación que afecta a
   `booking`, y hacerla antes de que `booking` exista significa escribirla dos veces. Cancelar y
   crear de nuevo cubre el caso mientras tanto. Si estás de acuerdo, corrijo el roadmap para que
   deje de prometerlo en esta fase.

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

- **`10-diagrama-er.md`**: `lessons.status` pasa de cuatro valores a dos, si se aprueba.
- **`openapi.yaml`**: `bookedCount` se retira hasta la Fase 9; el `summary` de la cancelación
  promete una cascada que esta fase no puede hacer; `attendance` se sirve desde `booking`.
- **`09-roadmap-implementacion.md`**: promete «modificación» en esta fase; propongo quitarla.
- **`01-product-architect.md`**: la ventana de 24 horas aplicada al profesor, si se aprueba
  levantarla.

## Entrega

Un solo PR desde `fase-8-lesson`, con el análisis primero —este documento, validado antes de
escribir una línea— y la implementación después, como en la Fase 7.

**No se escribe código hasta que valides este documento**, y en particular las tres preguntas
abiertas: la ventana de cancelación, el tope de capacidad y si `FULL` se deriva o se almacena.
