# Informe de cierre — Fase 9 (`booking`)

Rama `fase-9-booking`, cinco commits sobre `main` (tras el PR #22). El análisis, con sus siete
decisiones validadas antes de escribir código y las que aparecieron al implementar, está en
`Documentos/_arquitectura/20-fase9-analisis-booking.md`. Este informe no lo repite: resume, da la
evidencia y deja escrito lo que queda pendiente.

## Verificación

```
cd TennisPlatformApp/backend
mvn verify
[INFO] Tests run: 339, Failures: 0, Errors: 0, Skipped: 0
[INFO] BugInstance size is 0
[INFO] BUILD SUCCESS
```

De 290 a 339 tests. **Ninguno saltado.** SpotBugs sin hallazgos y sin tocar
`spotbugs-exclude.xml`.

**El criterio de salida se comprobó rompiendo lo que prueba.** `LastSeatConcurrencyTest` suelta
dos reservas de la última plaza con el mismo `CountDownLatch`, cinco veces. Pasa. Para saber si
eso significa algo se comentó el `@Lock(PESSIMISTIC_WRITE)` de
`LessonJpaRepository.findByIdForUpdate` y se volvió a ejecutar:

```
[ERROR] LastSeatConcurrencyTest.twoStudentsRaceForTheLastSeatAndExactlyOneWins <<< FAILURE!
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0
```

Las dos reservas entraron. Con el cerrojo restaurado vuelve a pasar. El cerrojo es lo que gana la
carrera, no la suerte.

## Commits

| Commit | Qué |
|---|---|
| `docs(booking)` | El análisis, con las siete decisiones cerradas |
| `test(architecture)` | La regla que deja **implementar** el spi de otro módulo, y `PublicPortsTest` que la ve rechazar una llamada |
| `feat(lesson)!` | `422 LESSON_IN_THE_PAST` (decisión 6) |
| `feat(booking)!` | El módulo, el cerrojo, las dos cascadas, `bookedCount` y `FULL` |
| `docs(booking)` | Contrato y documentos de diseño alineados con lo implementado |

Los dos `!` son rupturas de contrato reales, aunque ningún cliente las sufra todavía: `POST
/teacher/lessons` rechaza ahora lo que antes aceptaba, y `BookingStatus` pierde `ATTENDED` y
`NO_SHOW`.

## Las decisiones, en una línea cada una

1. **Inversión de dependencias** para lo que va contra el grafo: `lesson` y `student` declaran
   `LessonBookings` y `StudentBookings`; `booking` las implementa. Un solo mecanismo para contar
   plazas y para las dos cascadas, visible en los constructores y que impide arrancar si falta.
2. **La asistencia en su propia columna.** En `status` habría sacado la reserva de todo filtro
   `CONFIRMED` a la vez.
3. **Sin trigger de capacidad.** El cerrojo y el test bastan, y el test lo demuestra.
4. **Se puede volver a reservar** tras cancelar.
5. **Se puede reservar hasta el inicio.** Quien reserva con menos de 24 horas no podrá cancelar;
   el frontend debe avisarlo.
6. **No se crean clases en el pasado.**
7. **El admin cancela reservas sin ventana.**

## Lo que no estaba en el análisis

- **`403 EMAIL_NOT_VERIFIED`.** El análisis daba por hecho que el token garantizaba el email
  verificado; el login acepta cuentas sin verificar. Reservar es el único sitio donde la regla
  puede cumplirse.
- **`teacher_user_id` copiado en la reserva**, para no hacer `JOIN` contra la tabla de otro
  módulo en cada consulta del profesor.
- **`AUTH_FORBIDDEN` y `VALIDATION_ERROR` reutilizados** en vez de inventar códigos.
- **Un enum de dominio como `@RequestParam` responde 500** ante un valor desconocido. El filtro
  llega como texto.
- **`:param is null` con un UUID falla en PostgreSQL.** Los filtros opcionales van con
  `Specification`.

## Lo que se corrigió de documentos anteriores

- `10-diagrama-er.md`: el DDL de `bookings` (asistencia separada, profesor copiado, sin trigger).
- `01-product-architect.md`: los estados de la reserva.
- `01-analisis-funcional.md` §10: seguía atando al profesor a la ventana de 24 horas. La Fase 8
  corrigió el documento de producto y se dejó éste. §17: dos pendientes cerradas.
- `03-modelo-de-datos.md`: el solapamiento del alumno pasa de cerrojo a restricción de exclusión.
- `02-arquitectura.md` §4: `booking → identity` y el mecanismo spi.
- `openapi.yaml` y `11-contrato-api.md`: siete códigos nuevos, `bookedCount`, `attendance`,
  `lessonId`, y todas las notas de "pendiente hasta la Fase 9".
- Los dos `CLAUDE.md`: estado, trampas nuevas y el grafo.

## Problemas encontrados al implementar

- **Un script de edición en bash falló por el entrecomillado** y no escribió nada. Se detectó
  porque se comprobó qué archivos existían antes de seguir; a partir de ahí, las ediciones
  largas se hicieron con scripts en archivo.
- **`as` es palabra reservada en JPQL** y se había usado como nombre de parámetro. Se renombró
  antes de ejecutar nada.
- **El documento ER numera sus changelogs distinto que los ficheros** (`v7-booking` es su
  "Changelog 6"). Se mantuvo su numeración y se anotó.

## Riesgos y deuda pendiente

- **`updated_at` no se actualiza** ni en `lessons` ni en `bookings`: tiene `DEFAULT now()` y
  ninguna entidad la mapea. Nadie la lee. O se mapea o se quita, pero no debería seguir
  mintiendo.
- **Una cuenta deshabilitada conserva su access token** hasta que caduca, y en ese margen podría
  reservar. No es nuevo ni propio de `booking`: afecta a todo endpoint autenticado. La
  desactivación de un alumno por el profesor **sí** se comprueba contra la base en cada reserva.
- **La modificación de clases** tendrá que actualizar las copias de instantes de las reservas en
  la misma transacción. Está escrito en el changeset.
- **`TeacherRoleRequiredException` va por su quinta copia.** Es forzada por las reglas de
  frontera y está justificada en cada una, pero cinco copias de lo mismo merecen que la revisión
  de arquitectura de la fase 16 lo mire.

## Seguridad

Sin hallazgos. Revisado:

- **Rol y propiedad en los cuatro endpoints.** Reservar: rol alumno, id del token, gestión
  comprobada contra la base. Listar: el alumno ve las suyas; el profesor, las de sus clases, y un
  `lessonId` ajeno da una página vacía. Cancelar: el alumno las suyas, el profesor las de sus
  clases, el admin cualquiera; lo ajeno responde 404. Asistencia: rol profesor y cada reserva
  suya y de esa clase.
- **Ningún id de propiedad sale del cuerpo** de la petición.
- **Sin SQL dinámico**: JPQL con parámetros, y los nombres de campo de las `Specification` son
  constantes.
- **Nada se registra en logs** en `booking`.
- La clase dentro de una reserva **no lleva `notes`**.

## Cómo ejecutarlo

```
cd TennisPlatformApp/backend
mvn verify                                   # lo mismo que CI
mvn test -Dtest=LastSeatConcurrencyTest      # sólo la carrera por la última plaza
```

Docker tiene que estar en marcha, o los tests de integración se saltan y CI falla por ello.

## Qué revisar antes de continuar

- Que el CI del PR esté en verde **antes** de fusionar: `main` sigue sin protección.
- La decisión 5 tiene una consecuencia de producto que conviene tener presente al diseñar el
  frontend: reservar con menos de 24 horas deja al alumno sin poder cancelar.
- La siguiente fase del roadmap es `calendar`, que heredará el tope de 62 días y es el primer
  módulo que sólo lee.
