# Informe de cierre — Fase 7: disponibilidad del profesor

Rama `fase-7-availability`. Análisis validado por Daniel y después implementado. Fichero de
trabajo, no versionado.

## Qué se ha construido

El módulo `availability` completo: dominio, puertos, servicios, adaptadores, migración y
pirámide de tests. Cuatro endpoints y **un quinto entregable que no se ve desde fuera**, el
puerto de consulta que `lesson` (Fase 8) y `calendar` (Fase 10) usarán.

| Endpoint | Quién | Respuesta |
|---|---|---|
| `GET /teacher/availability?from=&to=` | Autenticado | Reglas semanales + excepciones del rango |
| `PUT /teacher/availability/weekly` | `TEACHER` | Reemplaza el conjunto completo |
| `POST /teacher/availability/exceptions` | `TEACHER` | 201 con la excepción creada |
| `DELETE /teacher/availability/exceptions/{id}` | `TEACHER` | 204 |

## Las decisiones que importan

### La interpretación de las reglas vive dentro del módulo

Es la decisión estructural. `lesson` y `calendar` **no leen reglas**: preguntan a
`QueryAvailability` por instantes ya resueltos. Si cada uno interpretara las reglas por su
cuenta habría tres implementaciones de una sola regla, y acabarían divergiendo: el calendario
pintaría un hueco donde crear la clase da error.

`AvailabilitySchedule` hace esa resolución y es **pura** —sin Spring, sin repositorio, sin
reloj—, que es lo que permite tener un test por cada transición horaria costando milisegundos
en vez de un contenedor.

Detalle que el análisis ya había señalado y que se cumplió: la regla `calendarOnlyUsesQueryPorts`
habría rechazado un puerto llamado `IsIntervalAvailable`. Se llama `QueryAvailability`.

### Horario de verano: comportamiento fijado, no modelado

Se usa la resolución de `ZonedDateTime` (hueco → desplaza hacia adelante; solapamiento → primer
desplazamiento) en vez de modelar la ambigüedad, que sería sobreingeniería. A cambio, **las
fechas de transición están escritas literales** en los tests: un test que le pregunta a
`java.time` cuándo cambia la hora se da la razón a sí mismo por construcción.

Lo que el análisis no había previsto: **un intervalo puede resolverse a duración cero**. Una
regla de 02:00 a 03:00 el día que los relojes se adelantan tiene los dos extremos en el mismo
instante. Construir un intervalo con eso lanzaría, y una lectura del calendario respondería 500
por cómo está hecho el año. Se descarta el intervalo, y tiene su test.

### Cuatro documentos corregidos

No son erratas: son decisiones tomadas aquí que dejan desfasado lo escrito antes.

1. **`day_of_week` pasa a ISO-8601 (1 = lunes … 7 = domingo).** `10-diagrama-er.md` decía
   `BETWEEN 0 AND 6` y `openapi.yaml` anotaba "0 = lunes", mientras que `java.time` numera el
   lunes 1 y `EXTRACT(DOW)` de PostgreSQL numera el **domingo** 0. Tres convenciones, en las
   tres capas que tocan el campo, donde elegir mal **no falla**: mueve el horario un día. La API
   expone el **nombre** (`MONDAY`), así que el número no sale del adaptador de persistencia.
2. **`GET /teacher/availability` gana rango obligatorio, tope de 62 días.** Prometía excepciones
   "vigentes" sin definir la palabra y sin nada que impidiera que la lista creciera para
   siempre. El número convierte en decisión el "p. ej. 62 días" que `11-contrato-api.md` dejaba
   suelto para `/calendar`.
3. **El grafo de módulos gana `availability → identity`.** Cualquier módulo con adaptador web
   necesita `AuthenticatedUser`. `teacher` y `student` ya dependían de `identity` por esto
   mismo; el documento simplemente nunca lo dijo, porque además tenían otras razones. `lesson` y
   `booking` necesitarán la misma arista.
4. **Las horas se serializan `HH:mm:ss`, no `HH:mm`.** Lo encontró la prueba manual contra el
   stack, no un test. Se corrige el spec y las horas pasan a un schema `LocalTime` que explica
   que son **hora de pared**, no instantes — que es el malentendido que importa evitar aquí.

### Dos cosas que SpotBugs enseñó

- **Rechazó el nombre `DateAvailabilityException`** (`NM_CLASS_NOT_EXCEPTION`). Tenía razón, y
  era justo la ambigüedad que yo quería quitar renombrando. El dominio usa `AvailabilityOverride`;
  **el contrato de la API no cambia** —campos `exceptions`, schema `AvailabilityException`,
  código `AVAILABILITY_EXCEPTION_NOT_FOUND`— y los DTOs de la capa web conservan el nombre del
  contrato, porque su trabajo es parecerse a él.
- **Marcó `EI_EXPOSE_REP2` en los cuatro servicios** por guardar los puertos con los que se
  construyen. Eso no tiene arreglo: un puerto de repositorio es un colaborador compartido por
  diseño. Lo que merece constar es que **el detector es inconsistente**: `ManageStudentService`,
  `GetTeacherProfileService` y `GetStudentLimitService` guardan sus puertos exactamente igual y
  no se marcan. La exclusión está acotada por regex a esas cuatro clases, para que no pueda
  tapar un hallazgo real. `AvailabilityView`, que sí guardaba listas mutables, **se arregló**
  copiándolas en vez de excluirse.

### Reglas solapadas: aplicación, no base de datos

Asimetría deliberada con `lessons`. Allí el solapamiento nace de una carrera que dos reservantes
pueden correr a la vez; aquí hay un solo profesor y el conjunto entero llega en una petición. Se
valida entero **antes de escribir nada**, de modo que una petición rechazada deja la
configuración anterior intacta — y eso tiene test, en dominio y sobre HTTP.

Y lo que se cuela en todas las implementaciones: **tocarse no es solaparse**. `09:00–11:00` y
`11:00–13:00` son válidas.

## Evidencia

### `mvn verify` completo

```
[INFO] Tests run: 227, Failures: 0, Errors: 0, Skipped: 0
[INFO] BugInstance size is 0
[INFO] Error size is 0
[INFO] BUILD SUCCESS
```

**Saltados: 0.** De 167 tests a 227: 60 nuevos.

| Clase | Tests | Qué cubre |
|---|---|---|
| `AvailabilityScheduleTest` | 11 | Resolución, invierno/verano, las dos transiciones, bloqueos y extras |
| `WeeklyAvailabilityRuleTest` | 9 | Adyacencia, conflicto, vigencia, día por nombre |
| `LocalTimeRangeTest` | 6 | Unión, resta, medio abierto |
| `AvailabilityOverrideTest` | 5 | `BLOCK` de día completo, `EXTRA` con horas obligatorias |
| `AvailabilityDateRangeTest` | 4 | El tope de 62 días |
| `ConfigureWeeklyAvailabilityServiceTest` | 6 | Solapes, propiedad, nada escrito al rechazar |
| `ManageAvailabilityExceptionsServiceTest` | 5 | Borrado acotado por profesor, validación |
| `QueryAvailabilityServiceTest` | 2 | El puerto que consumirán `lesson` y `calendar` |
| `TeacherAvailabilityApiTest` | 12 | Los cuatro endpoints sobre HTTP real + PostgreSQL |

### Esquema real, leído de la base después de migrar

```
                       Table "public.weekly_availability_rules"
     Column      |           Type           | Nullable |      Default
-----------------+--------------------------+----------+-------------------
 id              | uuid                     | not null | gen_random_uuid()
 teacher_user_id | uuid                     | not null |
 day_of_week     | smallint                 | not null |
 start_time      | time without time zone   | not null |
 end_time        | time without time zone   | not null |
 active_from     | date                     |          |
 active_until    | date                     |          |
Check constraints:
    "ck_weekly_availability_active_period" CHECK (active_from IS NULL OR active_until IS NULL OR active_from <= active_until)
    "ck_weekly_availability_hours" CHECK (start_time < end_time)
    "weekly_availability_rules_day_of_week_check" CHECK (day_of_week >= 1 AND day_of_week <= 7)
Foreign-key constraints:
    ... FOREIGN KEY (teacher_user_id) REFERENCES teacher_profiles(user_id)

                        Table "public.availability_exceptions"
Check constraints:
    "availability_exceptions_type_check" CHECK (type::text = ANY (ARRAY['BLOCK','EXTRA']))
    "ck_availability_exception_hours" CHECK (start_time IS NULL AND end_time IS NULL
        OR start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
```

El `BETWEEN 1 AND 7` está donde tiene que estar.

### Pruebas manuales contra el stack real

Backend reconstruido (`docker compose up -d --build backend`), Liquibase aplicó los 11
changesets y `{"status":"UP"}`.

| Prueba | Resultado |
|---|---|
| `PUT` con dos reglas del lunes | 200, dos reglas con id |
| `PUT` con dos reglas del martes solapadas | 400 `AVAILABILITY_RULES_OVERLAP`, con el par nombrado en el detalle |
| `PUT` con `"dayOfWeek": "0"` | 400 `AVAILABILITY_INVALID`, "Expected MONDAY to SUNDAY" |
| `POST` bloqueo de día completo | 201, `startTime` y `endTime` a null |
| `POST` `EXTRA` sin horas | 400 `AVAILABILITY_INVALID` |
| `GET` rango de un mes | 200, reglas + la excepción |
| `GET` rango de un año | 400 `AVAILABILITY_RANGE_TOO_WIDE`, "62 days, and this one is 365" |

## Revisión de seguridad

**Sin hallazgos.** Lo comprobado y descartado, resumido:

- **Escritura por un no-profesor**: dos barreras independientes, y ninguno de los tres
  servicios se salta la segunda. El rol se comprueba en el controlador; la **propiedad** en el
  servicio, exigiendo fila en `teacher_profiles`. Un token con rol `TEACHER` pero sin perfil
  recibe 403.
- **Identidad desde la petición**: no. El `teacherUserId` de toda escritura sale de
  `caller.id()`, y ningún DTO de entrada tiene campo de propietario, así que no hay
  *mass assignment*. El único `@PathVariable` es el id a borrar, y va como predicado.
- **`deleteByIdAndTeacher` perdiendo el segundo predicado**: verificado hasta el parser de
  Spring Data. Los dos predicados se emiten.
- **Fuga en el `GET` que lee cualquier autenticado**: las vistas llevan id, día, horas y
  vigencia. El `teacherUserId` se queda en el dominio y no se mapea nunca a la salida.
- **SQL injection**: el changeset es DDL estático y todas las consultas son derivadas de Spring
  Data. No hay JPQL ni SQL nativo escritos a mano.
- **La exclusión de SpotBugs**: acotada por regex a cuatro clases y solo a `EI_EXPOSE_REP2`; no
  abarca dominio, adaptadores ni DTOs.
- **Logs**: el módulo no tiene ningún logger.

### Un arreglo que salió de la revisión

Anotó —sin llegar a reportarlo, porque no es explotable— que `QueryAvailabilityService`
resolvía la zona con `byUserId(id).orElseGet(teacherProfile::get)`: un id que no fuera de
ningún profesor **tomaba prestado el reloj del profesor único** en vez de decir que no tiene
horario.

Con un solo profesor ese camino es inalcanzable, y eso es justo lo que lo hacía peligroso:
nada lo habría detectado nunca. El día que haya un segundo profesor resolvería las reglas de
uno contra el desplazamiento de otro — una respuesta **equivocada**, no un fallo, apareciendo
como un calendario desplazado una hora mucho después de la línea que lo causó.

Ahora `covers` devuelve `false`, `intervals` devuelve vacío, y no se consulta ningún
repositorio. Tiene su test. Va en un commit aparte.

## Riesgos y deuda

- **No hay frontend**: la pantalla de configuración de disponibilidad es la Fase 11. El roadmap
  la lista dentro de su Fase 3; aquí se entrega solo el backend, como en las fases anteriores.
- **`active_from` / `active_until` no tienen interfaz**: se respetan si llegan, pero ningún
  requisito pide todavía programar un cambio de horario con antelación. Se mantienen porque ya
  estaban diseñados y cuestan dos columnas anulables.
- **El `PUT` semanal destruye lo que no se reenvía**, incluida una regla con `activeFrom`
  futuro. Es la semántica del contrato y está documentada en tres sitios, pero el frontend de la
  Fase 11 tiene que leer-editar-escribir el conjunto entero o perderá datos en silencio.
- Sigue en pie la deuda de la Fase 6: **desactivar un alumno no cancela sus reservas futuras**,
  porque `booking` no existe. Tiene fase asignada y criterio de salida.

## Qué falta para cerrar la fase

La sesión se quedó **sin cuota** (`rate_limit`, HTTP 429) con cinco agentes en marcha, así que
lo de abajo no llegó a ejecutarse. No es un fallo del código.

1. **`/simplify`** —los cuatro agentes: reuse, simplificación, eficiencia y altitud—. Es la
   revisión que la metodología pide al cerrar fase completa y es lo único realmente pendiente.
   Modifica código, así que va antes del PR. Cuatro cosas concretas que debería mirar están
   listadas en `handoff-siguiente-sesion.md`.
2. **Empujar la rama y abrir el PR.** Mirar el check antes de fusionar.
3. Lo siguiente sería la **Fase 8, `lesson`**, que es la primera consumidora de
   `QueryAvailability`.

Un `/security-review` relanzado después de la revisión que ya pasó limpia quedó también a
medias, pero **es redundante**: cubre la misma rama más el commit del *fallback*, que solo hace
la respuesta más estricta.
