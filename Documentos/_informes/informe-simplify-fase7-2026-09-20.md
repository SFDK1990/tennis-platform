# Informe de `/simplify` — Fase 7 (`availability`)

20/09/2026. Revisión de calidad sobre `main...fase-7-availability`, con los cuatro ángulos que
pide la metodología: reutilización, simplificación, eficiencia y altitud. **No es una revisión
de corrección**: no se buscaron bugs, eso es `/code-review`.

Los cambios están **aplicados y sin commitear**, en el árbol de trabajo de `fase-7-availability`.

## Resultado de la verificación

```
[INFO] Tests run: 227, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotbugs:4.10.4.1:check (default) @ backend ---
[INFO] BugInstance size is 0
[INFO] Error size is 0
[INFO] No errors/warnings found
[INFO] BUILD SUCCESS
[INFO] Total time:  02:36 min
```

Mismo número de tests que antes de la revisión (227), ninguno saltado, y `ModuleBoundariesTest`
con sus 24 reglas en verde. Ningún cambio de comportamiento: todo lo aplicado es reorganización
interna o documentación.

## Lo que se ha cambiado, y por qué

### 1. El cuerpo del error estaba copiado en cuatro sitios

`AvailabilityExceptionHandler` traía una cuarta copia, carácter por carácter, del método privado
que construye el `ProblemDetail`. Las otras tres estaban ya en `identity`, `teacher` y `student`.

Se ha extraído a `com.tennisplatform.error.Problems.of(status, title, detail, code)` y los cuatro
`@RestControllerAdvice` lo llaman. `error` está fuera del grafo de módulos por decisión previa
(lo dice el javadoc de `ModuleBoundariesTest`), así que un adaptador puede importarlo sin tocar
ninguna regla de frontera.

Lo que arregla no es la longitud: es que el nombre de la propiedad `code` —sobre la que conmuta
el frontend— estaba escrito cuatro veces, y la quinta copia la iba a escribir la Fase 8.

**Esto toca tres módulos que ya están en `main`.** Es el único cambio del lote que sale del
módulo `availability` por el lado del código de producción. Es mecánico y la suite lo cubre,
pero amplía el alcance del PR de la Fase 7 y conviene que lo sepas antes de abrirlo.

### 2. La fontanería HTTP de los tests estaba copiada en cuatro clases

`bearer`, `tokenOf` y `tokenOfANewStudent` estaban repetidos en `TeacherAvailabilityApiTest`,
`TeacherProfileApiTest`, `TeacherStudentsApiTest` y `MeApiTest`. Las copias ya habían empezado a
discrepar en detalles que nadie decidió: cuál era la constante de la contraseña, si el helper
era estático, si llevaba `@SuppressWarnings("rawtypes")` o `("unchecked")`.

Ahora viven en `AbstractIntegrationTest`, que es donde ya vive el resto de infraestructura común,
junto con un `protected TestRestTemplate rest` compartido. La contraseña pasa como parámetro, que
es lo único que variaba de verdad entre las copias. Se ha añadido `jsonBearer` porque tres de las
cuatro clases hacían a mano el mismo `bearer` + `setContentType`.

Como consecuencia se ha quitado el `@Autowired private TestRestTemplate rest` de ocho clases de
test, que ahora lo heredan. `HealthEndpointTest` lo llamaba `restTemplate` y ahora usa el
heredado. `AuthRateLimitTest` sigue sustituyendo la `RequestFactory` en su `@BeforeEach`: es el
mismo bean del contexto que ya mutaba antes, así que su comportamiento no cambia.

### 3. `TeacherSchedules.requireTheTeacher` devolvía una zona horaria que nadie leía

Devolvía `ZoneId`, y sus tres llamadas —una en `ConfigureWeeklyAvailabilityService`, dos en
`ManageAvailabilityExceptionsService`— lo descartaban. Leía el perfil, parseaba la zona y tiraba
el resultado. Ahora es `void` y solo comprueba la propiedad, que es lo único que hacía de verdad.
Se ha ajustado el javadoc de la clase, que prometía devolver la zona.

### 4. La regla del día de margen estaba escrita dos veces

`AvailabilitySchedule.covers` y `QueryAvailabilityService.scheduleAround` calculaban por separado
las mismas dos líneas: un día de margen a cada lado porque la fecha local de un instante depende
del desplazamiento en vigor. Una decidía qué cargar de la base, la otra qué resolver.

Es exactamente el modo de fallo contra el que avisa el javadoc del propio `AvailabilitySchedule`:
si las dos se separan, el horario se lee sobre una ventana más estrecha que aquella contra la que
se resolvió, y nada falla. La regla vive ahora en el dominio, como `firstDayAround` y
`lastDayAround`, y la usan los dos.

### 5. Nombres totalmente cualificados dentro de métodos

`java.util.Optional` (seis veces), `java.util.Comparator`, `java.time.DayOfWeek` en una firma de
un fichero que ya importaba `DayOfWeek`, y `java.util.Locale.ROOT` en dos sitios. Sustituidos por
sus imports. Es solo ruido, pero estaba en los métodos más densos del módulo.

### 6. Tres decisiones que estaban tomadas por omisión, ahora escritas

Ninguna cambia código. Las tres son preguntas que dejaste abiertas en el handoff.

- **El coste de `AvailabilitySchedule.resolve`** está ahora en su javadoc: cada día reescanea la
  lista entera de excepciones, lo que sobre el papel es cuadrático, y se deja así a propósito.
  `AvailabilityDateRange` limita el rango a 62 días y el repositorio entrega solo las excepciones
  de esos mismos días, así que los dos factores del producto están acotados por los mismos dos
  meses. Son unos pocos miles de comparaciones en memoria, muy por debajo de las dos consultas
  que ya hicieron falta para cargar los datos.
- **Por qué la regla de ArchUnit se ensancha módulo a módulo** y no se generaliza, en el javadoc
  de `availabilityDependsOnTeacherAndIdentity`. Una regla general "todo módulo puede depender de
  `identity`" abriría un agujero en `platformDependsOnNoModule` y `sharedDependsOnNoModule`, cuyo
  trabajo es prohibir exactamente eso para que los demás puedan leerlos sin ciclo. Además
  autorizaría por adelantado una arista a cuatro módulos cuya fase no está analizada. La
  repetición es el mecanismo, no un defecto: un módulo nuevo queda denegado por defecto.
- **La causa de la exclusión de SpotBugs**, medida en vez de supuesta (siguiente apartado).

## La exclusión de SpotBugs: medida, y se queda

Dejaste anotado que el mismo patrón no se marca en `ManageStudentService`,
`GetTeacherProfileService` ni `GetStudentLimitService`, y que no habías averiguado por qué.

Lo he comprobado ejecutando SpotBugs, no razonando sobre él. Puse una clase de usar y tirar en
`availability.application.service` con tres campos en un solo constructor:
`AvailabilityRuleRepository`, `AvailabilityOverrideRepository` y el `GetTeacherProfile` de
`teacher`. Con el fichero de exclusiones vaciado, el informe fue:

```
EI_EXPOSE_REP2  ConfigureWeeklyAvailabilityService  -> AvailabilityRuleRepository
EI_EXPOSE_REP2  GetAvailabilityService              -> AvailabilityOverrideRepository
EI_EXPOSE_REP2  GetAvailabilityService              -> AvailabilityRuleRepository
EI_EXPOSE_REP2  ManageAvailabilityExceptionsService -> AvailabilityOverrideRepository
EI_EXPOSE_REP2  QueryAvailabilityService            -> AvailabilityOverrideRepository
EI_EXPOSE_REP2  QueryAvailabilityService            -> AvailabilityRuleRepository
EI_EXPOSE_REP2  SpotBugsProbeTmp                    -> AvailabilityOverrideRepository
EI_EXPOSE_REP2  SpotBugsProbeTmp                    -> AvailabilityRuleRepository
HRS_REQUEST_PARAMETER_TO_HTTP_HEADER  CorrelationIdFilter
```

La clase sonda se marca en sus dos campos de `availability` y **no** en el de `teacher`, con la
forma de la clase idéntica para los tres. Es decir: **no hay causa estructural del lado de los
servicios**. Lo que decide es el tipo del campo, así que no existe ninguna manera de escribir
esas cuatro clases que satisfaga al detector, y la exclusión es la respuesta honesta y no un
atajo. La sonda se borró y el fichero de exclusiones se restauró antes de la verificación final.

La justificación dentro de `spotbugs-exclude.xml` recoge ahora esta medición.

**Lo que no he hecho, y es decisión tuya**: ensanchar la exclusión a todo
`*.application.service.*`. El argumento a favor es que `lesson`, `booking`, `calendar` y
`administration` van a tropezar con lo mismo y cada fase añadirá otra línea de nombres de clase.
El argumento en contra —y por eso lo he dejado acotado— es que `GetTeacherProfile` demuestra que
el detector *no* marca todos los puertos: una exclusión por paquete taparía casos que hoy sí se
reportan, y la que hay acotada seguiría cazando un `EI_EXPOSE_REP` de verdad en un campo mutable.
Si prefieres ensancharla cuando llegue el tercer módulo con el mismo problema, es un cambio de
una línea.

## Lo que se ha revisado y se deja como está

- **`TeacherRoleRequiredException` duplicada en `student` y `availability`**, con
  `NotTheTeacherException` en `teacher` haciendo lo mismo. **La duplicación está forzada**, no es
  un descuido: `crossesOnlyThroughInboundPorts` prohíbe que un módulo dependa de nada de otro que
  no sea su `application/port/in`, y una excepción de dominio es justamente eso. Subirla a
  `shared` obligaría a que `shared` dejara de estar vacío de negocio. Queda un detalle menor y
  anterior a esta fase: el literal `"TEACHER_FORBIDDEN"` está escrito en tres handlers y podría
  ser una constante. No lo he tocado porque `availability` solo sigue lo que ya hacían los otros
  dos, y arreglarlo aquí sería arreglar algo de la Fase 6 dentro del PR de la 7.
- **Los `@NotNull` de los DTOs de entrada.** La revisión propuso quitarlos porque el dominio ya
  rechaza esos nulos, con el argumento de que la respuesta sería incoherente. **El argumento no
  se sostiene**: `GlobalExceptionHandler.handleValidation` sí existe y convierte el fallo de Bean
  Validation en un 400 con `code: VALIDATION_ERROR` y la lista de campos. Quitar las anotaciones
  cambiaría una respuesta que documenta `openapi.yaml`, y lo haría solo en este módulo. Se queda.
- **El boilerplate de parseo de enum** en `WeeklyAvailabilityRule.parseDayOfWeek` y
  `AvailabilityOverrideType.parse`. Un helper genérico necesitaría parámetros de tipo y un
  proveedor de mensaje: añadiría más máquina de la que quita con dos usos. Si aparece un tercero
  en `lesson` o `booking`, entonces sí.
- **Los dos adaptadores de persistencia.** `replaceAllForTeacher` hace un borrado a granel más un
  `saveAll`, y `deleteByIdAndTeacher` combina la comprobación de propiedad con el borrado en una
  sola sentencia. Sin N+1 y sin lecturas repetidas del perfil en ninguno de los cuatro servicios.
- **`rejectOverlaps`** es cuadrático a propósito y ya lo dice su comentario: un horario semanal
  son unas pocas filas por día de la semana.

## Una cosa para la Fase 8

`QueryAvailability.covers` e `intervals` aceptan `Instant from, Instant to` sin nada equivalente
al tope de 62 días que `GetAvailability` sí exige. Hoy no tiene consecuencia porque no hay ningún
llamador —`lesson` y `calendar` no existen—, pero el día que los haya, un rango amplio sí haría
trabajo proporcional. **El criterio de salida de la Fase 8 debería incluir qué rango máximo
acepta `QueryAvailability`**, o confirmar explícitamente que sus llamadores lo acotan.

## Ficheros tocados

Producción:

- `error/Problems.java` (nuevo)
- `identity/…/IdentityExceptionHandler.java`, `student/…/StudentExceptionHandler.java`,
  `teacher/…/TeacherExceptionHandler.java`
- `availability/adapters/in/web/AvailabilityExceptionHandler.java`
- `availability/application/service/TeacherSchedules.java`, `QueryAvailabilityService.java`
- `availability/domain/AvailabilitySchedule.java`, `WeeklyAvailabilityRule.java`,
  `AvailabilityOverrideType.java`
- `spotbugs-exclude.xml` (solo la justificación)

Tests:

- `AbstractIntegrationTest.java`, `HealthEndpointTest.java`
- `availability/TeacherAvailabilityApiTest.java`, `teacher/TeacherProfileApiTest.java`,
  `student/TeacherStudentsApiTest.java`, `web/MeApiTest.java`
- `error/SecurityErrorContractTest.java`, `identity/AuthFlowIntegrationTest.java`,
  `identity/AuthRateLimitTest.java`
- `architecture/ModuleBoundariesTest.java` (solo el javadoc)

## Qué revisar antes de seguir

1. Si aceptas que el PR de la Fase 7 toque los handlers de `identity`, `teacher` y `student`, o
   prefieres que eso salga en su propia rama y el PR quede limitado a `availability`.
2. Si quieres la exclusión de SpotBugs acotada como está o ensanchada al paquete.
3. No he hecho commit: los cambios están en el árbol de trabajo, listos para uno.
