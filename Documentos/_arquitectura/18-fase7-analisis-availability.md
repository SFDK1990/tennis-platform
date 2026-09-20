# Fase 7 — Análisis: disponibilidad del profesor

Corresponde a la **Fase 3 del roadmap** (`09-roadmap-implementacion.md`), que numera las fases de
otra manera. Cubre el módulo `availability`.

Este documento es el entregable de análisis: no se escribe código hasta que esté validado.

## Alcance

| Endpoint | Quién | Qué hace |
|---|---|---|
| `GET /teacher/availability` | Autenticado | Reglas semanales y excepciones de un rango de fechas |
| `PUT /teacher/availability/weekly` | `TEACHER` | Reemplaza el conjunto completo de reglas semanales |
| `POST /teacher/availability/exceptions` | `TEACHER` | Crea un bloqueo o una disponibilidad extraordinaria |
| `DELETE /teacher/availability/exceptions/{id}` | `TEACHER` | Elimina una excepción |

Y —esto es la mitad del trabajo, aunque no se vea desde fuera— **el puerto de consulta que
`lesson` y `calendar` usarán** para preguntar si un intervalo cae dentro de la disponibilidad.

Fuera de esta fase: clases, reservas, el calendario agregado y la pantalla de configuración
(Fase 11, frontend). El grafo de `02-arquitectura.md` permite `availability → teacher`, y hace
falta: la zona horaria del profesor vive en `teacher_profiles`.

## Decisiones

### La interpretación de las reglas vive en `availability`, y se expone como consulta

Es la decisión estructural de la fase. Hay dos formas de que `lesson` valide una clase contra la
disponibilidad:

1. `lesson` pide las reglas crudas y las interpreta.
2. `lesson` pregunta "¿está libre este intervalo?" y `availability` responde.

**Se elige la segunda.** Resolver una regla semanal a instantes concretos no es trivial —hay que
aplicar `active_from`/`active_until`, restar los `BLOCK`, sumar los `EXTRA` y convertir de hora
de pared a instante con la zona del profesor, incluyendo los días en que esa conversión no es
biyectiva—. Si esa lógica la ejecutan `lesson` y `calendar` por su cuenta, hay dos
implementaciones de una sola regla, y dos implementaciones de una regla divergen: será
cuestión de tiempo que el calendario pinte un hueco donde la creación de clase da error.

Consecuencia concreta que la implementación **no puede pasar por alto**: `ModuleBoundariesTest`
tiene la regla `calendarOnlyUsesQueryPorts`, que solo admite que `calendar` use puertos cuyo
nombre empiece por `Get`, `Find` o `Query`. Un puerto llamado `IsIntervalAvailable` —que es el
nombre que pide el cuerpo— **haría fallar esa regla**. El puerto se llamará `QueryAvailability`
o `GetAvailableIntervals`. No es cosmético: el nombre es el mecanismo de control.

### `day_of_week`: hay tres convenciones en juego y ninguna está fijada

`10-diagrama-er.md` declara `day_of_week SMALLINT CHECK (day_of_week BETWEEN 0 AND 6)` y
`openapi.yaml` anota "0 = lunes". Contra eso:

| Convención | Lunes | Domingo |
|---|---|---|
| `openapi.yaml` actual | 0 | 6 |
| `java.time.DayOfWeek` (ISO-8601) | 1 | 7 |
| PostgreSQL `EXTRACT(DOW FROM ...)` | 1 | **0** |

Tres numeraciones distintas para el mismo concepto, en las tres capas que van a tocar el dato.
Un error aquí **no rompe nada**: mueve la disponibilidad un día y produce un sistema que
funciona y miente.

**Decisión: se fija ISO-8601 (1 = lunes … 7 = domingo) en la base**, que es lo que devuelve
`DayOfWeek.getValue()` sin conversión, **y la API expone el nombre**, no el número:
`"dayOfWeek": "MONDAY"`. Un cliente puede equivocarse de número en silencio; no puede
equivocarse de nombre sin un 400.

Esto **modifica documentos ya escritos**, y por eso se señala en vez de arrastrarse:
`10-diagrama-er.md` (el `CHECK` pasa a `BETWEEN 1 AND 7`) y `openapi.yaml` (el campo pasa de
`integer` a `enum` de nombres). Es la clase de corrección que la metodología pide hacer en vez
de mantener por compatibilidad — y no hay compatibilidad que mantener: la tabla no existe
todavía.

### Hora de pared, zona del profesor, y qué pasa en los dos días raros del año

Una regla semanal dice "los lunes de 09:00 a 13:00". Eso es **hora de pared**, no un instante:
cuánto dura en tiempo real y a qué instante UTC corresponde depende de la zona horaria del
profesor (`teacher_profiles.timezone`, un id IANA) y de la fecha.

Dos días al año la conversión no es una función:

- **Adelanto (primavera):** las 02:00 no existen; el reloj salta a las 03:00. Una regla que
  cubra esa franja tiene una hora menos de tiempo real.
- **Atraso (otoño):** las 02:30 ocurren dos veces, con dos desplazamientos distintos.

Las opciones eran modelar la ambigüedad explícitamente o fijar un comportamiento y demostrarlo.
**Se fija el comportamiento**: la resolución usa `ZonedDateTime.of(...)`, que ante un hueco
desplaza hacia adelante la duración del salto y ante un solapamiento **elige el primer
desplazamiento**. No se añade configuración ni un modo alternativo: modelar la ambigüedad sería
sobreingeniería para un MVP con un profesor cuyas horas de trabajo realistas (mañana y tarde) no
cruzan la franja 02:00–03:00 en ninguna zona europea.

Lo que **sí** exige esta decisión es que deje de ser un accidente: hay un test por cada
transición que fija la salida. Si alguien cambia la resolución, falla un test con nombre propio
en lugar de aparecer una hora descolocada seis meses después.

El roadmap ya pedía estos tests ("cambios de horario de verano/invierno"). Aquí queda dicho qué
deben afirmar exactamente.

### El `PUT` semanal reemplaza **todo**, y el `GET` tiene que devolver todo lo que el `PUT` necesita

El contrato ya dice "reemplaza el conjunto completo de reglas semanales". Eso tiene una
consecuencia que conviene escribir antes de que muerda: **una regla con `activeFrom` en el
futuro desaparece si el cliente no la reenvía**. No hay fusión ni parcheo; lo que llega es lo
que queda.

Se mantiene esa semántica —es la que corresponde a una pantalla de "mi horario semanal", que
edita el conjunto entero— con una condición que la hace segura: **`GET /teacher/availability`
devuelve `activeFrom` y `activeUntil` de cada regla**, para que el ciclo leer→editar→escribir no
pierda datos en silencio. El schema `WeeklyAvailabilityRule` ya los incluye; queda anotado aquí
porque es lo que sostiene la decisión, no un detalle del DTO.

Se consideró eliminar `active_from`/`active_until` del MVP por YAGNI: ningún requisito pide
programar un cambio de horario con antelación. Se mantienen porque ya están diseñados, cuestan
dos columnas anulables, y sin ellos un cambio de horario obliga a editar el día exacto en que
entra en vigor. Pero **no se construye interfaz para ellos**: se respetan si llegan.

### Reglas solapadas: se rechazan en la aplicación, y no hay restricción en la base

Dos reglas del mismo día que se pisan (09:00–13:00 y 12:00–15:00) son un error del usuario, no
un dato válido. Se rechazan con `400 AVAILABILITY_RULES_OVERLAP`.

**Se valida en la aplicación y no con un `EXCLUDE USING gist`**, y esto es una asimetría
deliberada con `lessons`, que sí lleva su restricción de exclusión. La diferencia es la
concurrencia: dos alumnos pueden reservar a la vez y el solapamiento de clases nace de una
carrera que la aplicación no puede ganar sola. Aquí no hay carrera — hay **un solo profesor**,
el conjunto entero llega en una petición y se reemplaza en una transacción. Una restricción de
exclusión sobre `(teacher, día, rango horario)` acotada además por el rango de fechas de
vigencia no se expresa en el esquema sin desnormalizar, y compraría una garantía contra una
amenaza que no existe.

Detalle que hay que acertar y que se cuela en todas las implementaciones: **tocarse no es
solaparse**. `09:00–11:00` y `11:00–13:00` son válidas. El intervalo es cerrado por la izquierda
y abierto por la derecha.

### Excepciones: qué gana cuando se contradicen

El esquema admite que el mismo día tenga un `BLOCK` y un `EXTRA`. Nada decía cuál manda.

**Decisión: el `BLOCK` gana.** El orden de evaluación de un día es: reglas semanales vigentes →
se suman los `EXTRA` → se restan los `BLOCK`. Se elige así porque las dos lecturas no son
simétricas: interpretar mal un bloqueo pone al profesor a dar clase el día que había dicho que
no podía, mientras que interpretar mal un extra solo pierde un hueco que se puede volver a
añadir. Ante la duda, el sistema se equivoca hacia "no disponible".

Lo demás de las excepciones:

- `BLOCK` sin horas = día completo. `EXTRA` **exige** ambas horas. Se valida en la aplicación,
  como ya decía `10-diagrama-er.md`, para no codificar la regla en una combinación de `NULL`s.
- Las excepciones solapadas entre sí **no se rechazan**: dos `BLOCK` que se pisan bloquean lo
  mismo que uno, y dos `EXTRA` que se pisan suman el mismo hueco. Es idempotente; rechazarlo
  sería ceremonia.
- El puerto de borrado recibe el id del profesor además del de la excepción, aunque hoy solo
  haya un profesor y la comprobación sea trivial. Es la diferencia entre que la Fase 13 añada
  un `WHERE` y que tenga que reescribir la firma.

### La disponibilidad se lee sin ser profesor; se escribe solo siendo profesor

Mismo reparto que ya tiene el perfil del profesor, donde `GET /teacher/profile` lo puede llamar
cualquier autenticado y solo el `PATCH` está restringido. Un alumno necesita saber cuándo
trabaja su profesor: es el producto, no una fuga. Las tres mutaciones responden
`403 TEACHER_FORBIDDEN` a cualquier otro.

### `GET /teacher/availability` necesita un rango de fechas, y hoy no lo tiene

El contrato dice "reglas semanales y excepciones **vigentes**", sin definir vigentes y sin
parámetros. Las excepciones se acumulan indefinidamente: al cabo de dos temporadas esa respuesta
devuelve cientos de filas que nadie mira, y "vigentes" no significa nada comprobable.

**Decisión: `from` y `to` obligatorios, de tipo `date` (no `date-time`: una excepción es un día,
no un instante), acotados a un máximo de 62 días.** El rango filtra **las excepciones**; las
reglas semanales se devuelven enteras, porque son un conjunto pequeño y acotado (como mucho unas
pocas por día de la semana) y recortarlas por fecha solo complicaría el cliente.

El número sale de `11-contrato-api.md`, que para `/calendar` dice "acotado a un máximo razonable
(p. ej. 62 días)". Conviene ser honesto sobre qué es eso: una sugerencia, no una decisión —
`/calendar` ni siquiera tiene hoy el tope en `openapi.yaml`, y no lo tendrá hasta su fase. Esta
fase **convierte el "p. ej." en un número**, y `/calendar` heredará el mismo cuando llegue. Dos
topes distintos para la misma clase de consulta serían peor que cualquiera de los dos.

Modifica `openapi.yaml` y la sección de convenciones de `11-contrato-api.md`.

## Códigos de error nuevos

Con el criterio ya fijado en `11-contrato-api.md` (400 formato, 422 regla de negocio):

| Código | HTTP | Cuándo |
|---|---|---|
| `AVAILABILITY_RULES_OVERLAP` | 400 | Dos reglas del mismo día se pisan en el conjunto enviado |
| `AVAILABILITY_INVALID` | 400 | `end_time` no es posterior a `start_time`, un `EXTRA` sin horas, un día desconocido |
| `AVAILABILITY_EXCEPTION_NOT_FOUND` | 404 | El id no existe |
| `AVAILABILITY_RANGE_TOO_WIDE` | 400 | El rango pedido en el `GET` supera los 62 días |

Los cuatro son 400 o 404 y ninguno 422: todos son errores de forma o de referencia, no
violaciones de una regla de negocio sobre datos válidos.

## Criterios de aceptación

Medibles, según la regla 5 de la metodología. La fase no se cierra sin un test que demuestre
cada uno:

1. Una regla semanal definida en la zona del profesor se resuelve al instante UTC correcto en
   una fecha de horario de invierno **y** en una de verano, con la diferencia de desplazamiento
   esperada.
2. El día del **adelanto** de hora, una regla que cubre la franja del salto produce una hora
   menos de disponibilidad real, y el test fija ese resultado.
3. El día del **atraso**, una regla que cubre la franja repetida se resuelve con el primer
   desplazamiento, y el test fija ese resultado.
4. `PUT` con dos reglas solapadas del mismo día responde `400 AVAILABILITY_RULES_OVERLAP` y **no
   guarda ninguna de las dos**: el conjunto se valida entero antes de escribir.
5. `PUT` con dos reglas adyacentes (`09:00–11:00` y `11:00–13:00`) responde `200`.
6. Un `BLOCK` de día completo deja el día sin disponibilidad aunque existan reglas semanales y un
   `EXTRA` esa misma fecha.
7. Un `EXTRA` fuera del horario semanal aparece como disponible.
8. Un alumno autenticado puede leer la disponibilidad; un alumno que intenta escribirla recibe
   `403 TEACHER_FORBIDDEN`.
9. Una regla con `activeUntil` pasado no aparece en la disponibilidad de hoy.
10. `GET` con un rango mayor de 62 días responde `400`.
11. El puerto que consumirá `calendar` tiene un nombre que `calendarOnlyUsesQueryPorts` acepta
    — lo demuestra la propia regla de ArchUnit, que ya está en la suite.

## Estrategia de testing

La pirámide de siempre: dominio sin Spring, aplicación con puertos simulados, REST, integración
con Testcontainers. Dos cosas propias de esta fase:

**La resolución de reglas a instantes es lógica de dominio pura y se prueba sin Spring y sin
base de datos.** Recibe reglas, excepciones, una zona horaria y un rango; devuelve intervalos.
Los diez primeros criterios de aceptación, salvo los de HTTP, son tests de dominio de
milisegundos, no tests de integración. Es lo que hace asumible tener un caso por cada transición
horaria en vez de uno solo "representativo".

**Las fechas de las transiciones se escriben literales en el test, no se calculan.** Un test que
deduce cuándo cambia la hora usando la misma librería que está probando no prueba nada.

## Lo que hay que tocar y suele olvidarse

- **`AbstractIntegrationTest` tiene que vaciar `weekly_availability_rules` y
  `availability_exceptions`.** Ambas tienen clave ajena a `teacher_profiles`, así que
  `TRUNCATE ... CASCADE` las alcanzaría igualmente — y ese es justo el camino por el que la
  Fase 6 se llevó por delante `platform_configuration` sin que nadie lo viera. Van en la lista
  explícitamente.
- **Todo test de disponibilidad necesita un profesor sembrado.** La limpieza borra el que creó
  el bootstrap al arrancar el contexto. Ya está anotado en `CLAUDE.md`; aquí afecta a todos los
  tests de la fase sin excepción, porque la clave ajena es obligatoria.
- **El changelog de Liquibase es nuevo y append-only.** Es el "changelog 4 — availability" de
  `10-diagrama-er.md`, con la corrección de `day_of_week` ya aplicada.
- **Verificar el esquema contra la base real** después de migrar, y pegar la salida.

## Documentos que esta fase corrige

No son erratas: son decisiones que se toman aquí y dejan desfasado lo escrito antes.

| Documento | Qué cambia |
|---|---|
| `10-diagrama-er.md` | `day_of_week` pasa a ISO-8601, `CHECK (... BETWEEN 1 AND 7)` |
| `openapi.yaml` | `dayOfWeek` pasa a enum de nombres; `GET` gana `from`/`to` obligatorios y el 403 de las mutaciones |
| `11-contrato-api.md` | Los cuatro códigos nuevos y el límite de 62 días del rango |
| `TennisPlatformApp/CLAUDE.md` | El módulo `availability` deja de estar vacío |

## Entrega

Una sola entrega, `fase-7-availability`, con su Pull Request. A diferencia de la Fase 6 no hay
razón para partirla: es un módulo con una dependencia ya satisfecha.

La fase **no empieza a escribir código hasta que este documento esté validado**.
