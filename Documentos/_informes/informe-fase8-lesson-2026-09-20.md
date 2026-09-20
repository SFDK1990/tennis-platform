# Informe de cierre — Fase 8 (`lesson`)

20/09/2026. Rama `fase-8-lesson`, cuatro commits: el análisis, el cierre de sus decisiones, la
implementación y la limpieza de `/simplify`. El módulo de clases del profesor: crear, consultar,
listar y cancelar, validadas contra la disponibilidad que dejó lista la Fase 7.

El análisis previo, con las decisiones que se cerraron antes de escribir código y las que hubo
que cerrar al implementarlo, está en `Documentos/_arquitectura/19-fase8-analisis-lesson.md`.

## Verificación

`mvn verify` desde `TennisPlatformApp/backend`, al terminar la implementación. La cifra final,
después de la limpieza, es 284 y está al pie de este informe.

```
[INFO] Tests run: 283, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotbugs:4.10.4.1:check (default) @ backend ---
[INFO] BugInstance size is 0
[INFO] Error size is 0
[INFO] BUILD SUCCESS
```

**0 saltados**, que es el contador que importa: un test de integración que se salta parece verde
y no prueba nada. Las 24 reglas de `ModuleBoundariesTest` pasan, incluida la ensanchada en esta
fase.

57 tests nuevos respecto a los 227 de la Fase 7: 29 de dominio, 12 de aplicación con puertos
simulados, 15 de integración contra PostgreSQL real, y 1 más en `platform`.

### Esquema verificado contra la base real

No afirmado: consultado. El stack se reconstruyó con el código de esta fase y se preguntó a la
base.

```
 v5-availability-exceptions        | tennis-platform | EXECUTED |            11
 v6-btree-gist-extension           | tennis-platform | EXECUTED |            12
 v6-lessons                        | tennis-platform | EXECUTED |            13
 v6-platform-max-group-capacity    | tennis-platform | EXECUTED |            14
(14 rows)
```

```
Indexes:
    "lessons_pkey" PRIMARY KEY, btree (id)
    "ex_lessons_no_teacher_overlap" EXCLUDE USING gist (teacher_user_id WITH =, tstzrange(starts_at, ends_at) WITH &&) WHERE (status::text <> 'CANCELLED'::text)
    "ix_lessons_teacher_starts_at" btree (teacher_user_id, starts_at)
Check constraints:
    "ck_lessons_cancelled_at" CHECK ((status::text = 'CANCELLED'::text) = (cancelled_at IS NOT NULL))
    "ck_lessons_duration_multiple" CHECK (mod(EXTRACT(epoch FROM ends_at - starts_at)::integer, 1800) = 0)
    "ck_lessons_individual_capacity" CHECK (type::text <> 'INDIVIDUAL'::text OR capacity = 1)
    "ck_lessons_period" CHECK (starts_at < ends_at)
    "lessons_capacity_check" CHECK (capacity > 0)
    "lessons_status_check" CHECK (status::text = ANY (ARRAY['OPEN'::character varying, 'CANCELLED'::character varying]::text[]))
    "lessons_type_check" CHECK (type::text = ANY (ARRAY['INDIVIDUAL'::character varying, 'GROUP'::character varying]::text[]))
Foreign-key constraints:
    "lessons_teacher_user_id_fkey" FOREIGN KEY (teacher_user_id) REFERENCES teacher_profiles(user_id)
```

```
  extname
------------
 btree_gist
 pgcrypto
 plpgsql

 id | student_limit | max_group_capacity
----+---------------+--------------------
  1 |            50 |                  8
```

La restricción de exclusión existe y filtra por `status <> 'CANCELLED'`, `btree_gist` está
instalada, y el tope de capacidad quedó en 8. El backend responde `{"status":"UP"}`.

## Las decisiones, y por qué

### La fase se define por lo que `lesson` no puede ver

`booking` depende de `lesson`, nunca al revés. Contar plazas ocupadas no es algo que este módulo
pueda hacer, así que las cuatro cosas que el contrato le pedía y que dependen del recuento
—`bookedCount`, el estado `FULL`, el marcado de asistencia y la cascada al cancelar— se aplazan
a la Fase 9.

`bookedCount` **se retira del contrato** en vez de responderse con un cero constante. Un cero
parece un dato; la ausencia del campo parece lo que es. Hay precedente escrito en el propio
spec: `RegisterRequest` retiró `fullName` hasta la Fase 6 con este mismo argumento.

### `FULL` y `COMPLETED` se derivan al leer

La columna guarda `OPEN` y `CANCELLED`, que es lo que decide una persona. Un `FULL` almacenado
tendría que escribirlo `booking` y mantenerlo en paz con el recuento real; el día que discrepen,
la clase no falla: dice `OPEN` sin plazas, o `FULL` con una libre. `COMPLETED` se deduce del
reloj, lo que evita un proceso programado que el MVP no tiene.

Por el mismo criterio no hay columna `cancelled_within_window`: eso es `cancelled_at` restado de
`starts_at`.

Esto corrige `10-diagrama-er.md`, que preveía cuatro valores en la columna.

### Duración en instantes, medianoche en hora local

La duración se mide sobre el tiempo real transcurrido, que es lo que un `CHECK` puede comprobar.
Los dos días del año en que cambia la hora, eso significa que una clase que en el reloj del
profesor va de 01:30 a 03:30 dura **una hora**. Manda el instante; el frontend es quien debe
enseñar la duración resultante antes de confirmar.

«No cruza medianoche», en cambio, se evalúa en hora de pared con la zona del profesor, porque
medianoche es una idea local: la misma pareja de instantes cruza medianoche en Madrid y no en
Nueva York, y hay un test que lo demuestra. Las fechas de transición están escritas literales,
como en la Fase 7.

### El solapamiento se comprueba dos veces

En la aplicación, para poder responder `409` con un mensaje útil; y en la base con
`EXCLUDE USING gist`, porque entre la comprobación y el `INSERT` cabe otra petición —el profesor
con dos pestañas abiertas basta—. Quitar la primera deja respuestas incomprensibles; quitar la
segunda deja una carrera real.

La restricción necesita `btree_gist`, que se crea en su propio changeset porque una extensión es
un objeto de toda la base y `booking` la va a necesitar en la Fase 9.

### La ventana de 24 horas deja de atar al profesor

Tal como estaba escrita en `01-product-architect.md`, un profesor que enfermara la noche antes
**no podía cancelar su propia clase**: la única salida habría sido que un ADMIN lo hiciera por
él, y en este MVP el ADMIN no es una persona de guardia, es una cuenta.

La ventana existe para proteger al profesor de un hueco que ya no puede llenar. Eso justifica
atar al alumno que cancela su reserva y no justifica atar al profesor sobre su propia clase. Se
levanta para el profesor, se mantiene intacta para el alumno, y queda registrado si la
cancelación dejó menos de 24 horas para que la Fase 9 pueda avisar a los afectados.

Lo validaste explícitamente antes de implementarlo, porque cambia una regla de negocio escrita.

### El tope de capacidad de grupo, configurable

Hasta ahora la única regla era `capacity > 0`: un error de tecleo podía crear una clase de diez
mil plazas sobre una única pista. Ahora hay un tope, vive en `platform_configuration` junto al
límite de alumnos, y vale 8 por defecto. Configurable y no constante porque el día que el número
esté mal, lo estará para todas las clases a la vez.

Cuesta una arista en el grafo: `lesson → platform`. Es legítima —`platform` no depende de nada
justamente para que cualquiera pueda leerlo sin ciclo— y está añadida a `ModuleBoundariesTest`
junto con `lesson → identity`.

### Dos barreras de autorización, como en la Fase 7

El rol se comprueba en el controlador y la propiedad en el servicio. Ninguna sustituye a la
otra: el rol dice qué clase de cuenta llama, la propiedad dice que es la cuenta dueña de lo que
se toca. El `teacherUserId` sale siempre del token.

`notes` es del profesor y viaja como `null` para cualquier otro llamante. Es texto libre donde
el profesor escribe para sí mismo.

## Lo que se corrigió de documentos anteriores

- **`10-diagrama-er.md`**: `lessons.status` pasa de cuatro valores a dos; se añade `cancelled_at`
  y `platform_configuration.max_group_capacity`; se anota que `version` existe pero no se mapea.
- **`01-product-architect.md`**: la ventana de 24 horas deja de aplicarse al profesor.
- **`09-roadmap-implementacion.md`**: prometía «modificación» en esta fase; se quita, con el
  motivo escrito.
- **`11-contrato-api.md`**: siete códigos nuevos, y la explicación de por qué
  `CANCELLATION_WINDOW_EXPIRED` ya no aparece en la cancelación de una clase.
- **`openapi.yaml`**: `bookedCount` retirado, `GET /teacher/lessons` añadido, la cascada de la
  cancelación marcada como Fase 9, y el `summary` de `attendance` explicando que lo servirá
  `booking`.
- **`CLAUDE.md`** (los dos): tabla de fases, grafo de módulos, reglas de negocio y tres trampas
  nuevas.

## Problemas encontrados al implementar

**El contexto de Spring no arrancaba.** Al exponer el servicio de límites de `platform` a la vez
como bean de su clase concreta y como bean de cada uno de sus dos puertos, quedaban tres beans
del mismo objeto y cualquier inyección se volvía ambigua:
`NoUniqueBeanDefinitionException ... found 2: platformLimitsService, getMaxGroupCapacity`. Se
declara un único bean por su tipo concreto y la inyección por cualquiera de las dos interfaces
encuentra un solo candidato. Queda escrito en el documento de análisis porque es fácil de
repetir.

**El test de `PlatformSettings` dejó de compilar** al añadir el segundo límite. Se actualizó y se
le añadió el caso del tope nuevo.

## Riesgos y deuda pendiente

- **Nada impide crear una clase en el pasado.** Forzando la disponibilidad se puede, y nace
  `COMPLETED`. No se ha prohibido porque ninguna regla del producto lo pide y el análisis no lo
  planteó: rechazarlo habría sido inventar una regla. Puede ser útil —registrar una clase ya
  dada— o un error de tecleo que nadie detiene. **Merece decisión explícita en la Fase 9**,
  cuando el marcado de asistencia le dé un sentido u otro.
- **`QueryAvailability.covers` e `intervals` siguen sin acotar el rango.** En esta fase no se
  manifiesta porque una clase dura horas. Vuelve a mirarse en `calendar`.
- **`TeacherRoleRequiredException` va por la cuarta copia**, y `LessonDateRange` duplica
  `AvailabilityDateRange`. Las dos duplicaciones están forzadas por las reglas de frontera: un
  tipo de dominio no se puede compartir entre módulos.
- **La cascada al cancelar y el marcado de asistencia son criterio de salida de la Fase 9.** Hoy
  cancelar una clase no toca ninguna reserva, sencillamente porque no hay reservas.

## Cómo ejecutarlo

```
cd TennisPlatformApp/backend
mvn verify

cd TennisPlatformApp
docker compose up -d --build
```

El backend escucha en el 8081. Para probarlo a mano hace falta el token del profesor
(`POST /api/v1/auth/login`) y, para crear una clase dentro de horario, tener disponibilidad
configurada (`PUT /api/v1/teacher/availability/weekly`) — o pasar `overrideAvailability: true`.

## Qué revisar antes de continuar

1. La decisión sobre crear clases en el pasado, arriba.
2. Que el cambio de la ventana de cancelación en `01-product-architect.md` dice lo que querías.
3. El resultado de las dos revisiones de cierre de fase, abajo.

## Las dos revisiones de cierre

### Seguridad: sin hallazgos

Se comprobó, endpoint por endpoint, que el `teacherUserId` sale siempre del token y nunca del
cuerpo; que las dos barreras —rol en el controlador, propiedad en el servicio— están en los tres
endpoints de escritura; que las dos consultas JPQL usan parámetros y no concatenación; que el
changeset es DDL estático; y que no se registra ningún token, contraseña ni dato personal.

### `/simplify`: cuatro ángulos, dos hallazgos de fondo

**Reutilización: limpio.** Nada re-implementado. Las dos duplicaciones que hay
—`TeacherRoleRequiredException` y `LessonDateRange`— están forzadas por las reglas de frontera.

**Lo que se arregló, y lo que enseñó:**

El comentario del adaptador decía que la violación de la restricción se traducía comparando el
**nombre** de la restricción, y no era verdad: buscaba en el texto del mensaje. Al ir a
arreglarlo apareció algo que no se sabía: **Hibernate no rellena el nombre de la restricción
cuando la violada es de exclusión**. Reconoce claves primarias, ajenas, únicas y `CHECK`, y para
ésta informa `constraint [null]`. Se descubrió escribiendo primero el test y viéndolo fallar.

La traducción se apoya ahora en el **SQLState `23P01`** de PostgreSQL —un código estándar, no
prosa— y conserva el nombre como segunda condición, para que añadir otra restricción de
exclusión a esta tabla no se reporte como un solapamiento de clases.

**Y faltaba un test que debería haber estado.** Todas las pruebas de solapamiento las resolvía
la comprobación que el servicio hace antes de escribir, así que **todas habrían pasado igual si
la restricción no existiera**: probaban la aplicación, no el esquema. El test nuevo va directo
al repositorio, se salta esa comprobación y deja sólo la base entre las dos clases, que es la
situación que encontraría una segunda petición compitiendo con la primera.

El `GET` de una clase decidía si enseñar las notas del profesor **sólo por el rol**, cuando en
todo el resto del módulo se comprueban rol y propiedad juntos. Con un único profesor los dos
coinciden, así que ningún test lo habría cazado; el día que haya dos tampoco fallaría: le daría
a un profesor las notas del otro.

El resto fue borrar: `LessonPeriod.overlaps` parecía la comprobación de solapamiento y no la
usaba nadie salvo su propio test —peor que no estar—; `Lesson.period()` no tenía ningún uso; una
clase individual ya no lee la configuración de capacidad de grupo que no puede usar; y el
mensaje de "no existe esa clase" se construye una vez en lugar de dos.

**Lo que se dejó como está, con motivo:**

- El perfil del profesor se lee dos veces por `POST` (una en la comprobación de propiedad, otra
  dentro de `QueryAvailability`). No cuesta una segunda consulta: es la misma transacción y la
  misma búsqueda por clave primaria, así que la segunda la sirve la caché de primer nivel de
  JPA. Arreglarlo sería acoplar el servicio a las interioridades de `QueryAvailability` a cambio
  de nada.
- `LessonPeriod.duration()` sólo lo usan los tests. Se mantiene porque son precisamente los que
  documentan la decisión central de la fase —la duración en instantes frente al reloj— y
  calcularla a mano en cada aserción los haría menos legibles.
- **Deuda anotada**: el índice `(teacher_user_id, starts_at)` acota el recorrido por arriba pero
  no por abajo, porque `ends_at` no está en él. Con un profesor y ventanas de 62 días no se nota;
  cuando el volumen importe, la consulta se puede reescribir con `tstzrange && tstzrange` para
  que use el índice GiST que la restricción de exclusión ya crea. Es un cambio de una línea, sin
  migración.

### Verificación final tras la limpieza

```
[INFO] Tests run: 284, Failures: 0, Errors: 0, Skipped: 0
[INFO] BugInstance size is 0
[INFO] Error size is 0
[INFO] BUILD SUCCESS
```
