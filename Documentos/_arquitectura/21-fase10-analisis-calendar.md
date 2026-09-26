# Fase 10 — Análisis: el calendario (`calendar`)

Daniel pidió hacer el calendario antes del frontend, porque sin él un alumno no tiene forma de
ver qué puede reservar. Las decisiones se tomaron con las opciones por defecto que se le
propusieron; están aquí para validarlas en el PR.

## Alcance

`GET /api/v1/calendar?from=YYYY-MM-DD&to=YYYY-MM-DD`, para el profesor y para el alumno. Sólo
lectura: `calendar` no tiene tablas ni escribe en ningún módulo, y ArchUnit ya exige que sólo use
puertos `Get*`, `Find*` y `Query*`.

## Decisiones

1. **El rango son fechas, no instantes**, con el mismo tope de 62 días que el resto
   (`DateRange`), interpretadas en la zona del profesor. El borrador del contrato pedía
   `date-time`; un calendario se pide por días, y un día es una idea local.
2. **La respuesta es un objeto, no una unión de entradas.** El borrador devolvía una lista
   mezclada de `AVAILABILITY` y `LESSON` con un discriminador. Dos listas separadas —
   `availability` y `lessons`— más la zona del profesor son más fáciles de pintar y de tipar.
3. **El profesor ve** su disponibilidad resuelta en intervalos (lo que devuelve
   `QueryAvailability.intervals`, no las reglas) y todas sus clases del rango, canceladas incluidas.
4. **El alumno ve** las clases de los profesores que lo gestionan, **sin las canceladas** salvo
   que tuviera reserva en ellas —para que entienda por qué desaparece una clase—, y en cada una
   su propia reserva si la tiene. No ve la disponibilidad: lo que puede reservar son clases, no
   huecos.
5. **Un alumno no gestionado ve un calendario vacío**, no un error: todavía no hay nada que
   pueda reservar, y es exactamente lo que la pantalla tiene que decirle.
6. **Sin `notes`** para nadie: el calendario es un resumen. El profesor las lee en la clase.
7. **Un admin recibe `403 AUTH_FORBIDDEN`**: no tiene calendario.

## Qué necesita de otros módulos

- `teacher`: `GetTeacherProfile`, por la zona.
- `availability`: `QueryAvailability.intervals`, que existe desde la Fase 7 para esto.
- `lesson`: `GetLesson.forTeacherBetween`, que ya resuelve fechas en la zona del profesor.
- `student`: **nuevo** `QueryManagedStudent.teachersOf(alumno)`.
- `booking`: **nuevo** `GetBookings.ofStudentInLessons(alumno, clases)`.

## Criterios de aceptación

- El profesor recibe su disponibilidad en intervalos y todas sus clases, canceladas incluidas.
- El alumno gestionado recibe las clases no canceladas y, en la que ha reservado, su reserva.
- Una clase cancelada con reserva del alumno le aparece; una cancelada sin su reserva, no.
- Un alumno no gestionado recibe listas vacías.
- Un rango de más de 62 días responde `400 DATE_RANGE_INVALID`.
- Un admin recibe `403`.
- `calendarOnlyUsesQueryPorts` sigue en verde.
