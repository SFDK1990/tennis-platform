# Tennis Platform — Metodología de trabajo

Cómo se construye el proyecto y en qué punto está. **Es la única fuente del estado de las
fases**: ningún otro documento lo repite.

## Regla central

> **No se avanza de fase sin validación explícita de Daniel.**

El agente no encadena fases por iniciativa propia.

## Cómo se trabaja una fase

1. **Rama** `fase-<n>-<tema>`; lo que no es una fase va en `docs/<tema>`, `fix/<tema>` o
   `chore/<tema>`.
2. **Análisis antes de código**, en `NN-fase<n>-analisis-<modulo>.md`: requisitos, ambigüedades,
   decisiones con su porqué, contrato y criterios de aceptación medibles. Daniel lo valida.
3. **Implementación** en commits que dejan el proyecto compilando y en verde.
4. **Cierre en el propio PR.** El análisis gana un apartado "Decisiones tomadas al implementar".
   La descripción del PR es el informe de cierre: evidencia pegada, riesgos y qué revisar. **El
   PR marca la fase como completada en la tabla de abajo**; al fusionarlo, pasa a ser cierto. No
   hay informes aparte ni PR de "poner al día el estado".
5. Daniel fusiona **con el CI en verde**. `main` no se puede proteger en el plan actual, así que
   es la única barrera.
6. **Etiqueta.** Tras fusionar, `main` se etiqueta subiendo la versión menor (`v0.15.0`,
   `v0.16.0`…), sin relación con el número de fase. `v1.0.0` es el primer despliegue que usa
   Marcos; desde ahí, cada despliegue sale de una etiqueta, y un arreglo urgente va en una rama
   `hotfix/<tema>` que sale de esa etiqueta y vuelve también a `main`.

## Reglas

- **La evidencia se pega, no se afirma.** La salida real de `mvn verify`, mirando el contador de
  tests **saltados**, no sólo `BUILD SUCCESS`.
- **Un test que sólo se ha visto pasar no prueba nada.** Lo crítico se comprueba rompiéndolo.
- **Una mala decisión anterior se señala y se corrige**, no se mantiene por compatibilidad.
- **Una decisión vive en un solo sitio.** Si una fase la cambia, se cambia ahí; no se anotan
  correcciones en cada documento que la mencionaba.
- **Criterios de aceptación medibles**: "existe un test que demuestra X", no "implementa X".
- **El testing no es una fase**: cada fase trae sus tests.
- **Al empezar una sesión**, el estado se comprueba (`git log`, `gh pr list`) antes de resumirlo.
  Un resumen de memoria ya dio una vez por pendiente una fase que estaba fusionada.

Principios de diseño: SOLID, dependencias explícitas y ninguna complejidad sin una razón que se
pueda explicar. Kafka y la IA quedan fuera del MVP.

## Fases

| # | Fase | Estado | Criterio de salida |
|---|------|--------|--------------------|
| 1 | Análisis funcional | Completada | `01-analisis-funcional.md` |
| 2 | Arquitectura | Completada | `02-arquitectura.md` |
| 3 | Modelo de datos | Completada | `03-modelo-de-datos.md`, `10-diagrama-er.md` |
| 4 | Skeleton del backend | Completada | — |
| 5 | Seguridad y autenticación (`identity`) | Completada | `13-fase5-analisis-identity.md` |
| 5.1 | Integración continua | Completada | `14-fase5.1-integracion-continua.md` |
| 6 | Perfiles y alumnos (`teacher`, `student`) | Completada (PR #13, #15) | `16-fase6-analisis-perfiles.md` |
| 7 | Disponibilidad (`availability`) | Completada (PR #17) | `18-fase7-analisis-availability.md` |
| 8 | Clases (`lesson`) | Completada (PR #20) | `19-fase8-analisis-lesson.md` |
| 9 | Reservas (`booking`) | Completada (PR #23) | `20-fase9-analisis-booking.md` |
| 10 | Calendario (`calendar`) | Completada (PR #25) | `21-fase10-analisis-calendar.md` |
| 11 | Frontend | Completada | `22-fase11-analisis-frontend.md`. Daniel recorre en el navegador, con los dos roles, registro → verificación → gestión → clase → reserva → cancelación → asistencia. Después, cada fase trae su pantalla |
| 12 | Administración (`administration`) | Completada | `23-fase12-analisis-administracion.md`. Sólo `ADMIN` accede; ajusta el límite de alumnos y el estado de cualquier usuario |
| 13 | Revisión de API | Completada | `24-fase13-analisis-revision-api.md`. Revisión REST completa y `openapi.yaml` sin diferencias con lo implementado |
| 14 | Cobertura y E2E | Completada | `25-fase14-analisis-cobertura-e2e.md`. Playwright cubre los flujos críticos de los dos roles |
| 15 | Auditoría de seguridad | Completada | `26-fase15-analisis-seguridad.md`. OWASP Top 10, acceso horizontal, secretos y dependencias revisados |
| 15.5 | Experiencia y diseño | Completada | `27-fase15.5-analisis-experiencia-diseno.md`. Maqueta aprobada por Daniel antes de implementar; identidad visual, "Hoy" del profesor, próxima clase del alumno y pantallas vacías que guían. Sólo frontend, con los E2E como red |
| 16 | Observabilidad | Completada | `28-fase16-analisis-observabilidad.md`. Logs estructurados, métricas de reservas y conflictos, correlation id de punta a punta |
| 17 | Endurecimiento y despliegue | 17.1 en curso | `29-fase17-analisis-endurecimiento-despliegue.md`. La 17.1 (despliegue y backups) se retoma en una Raspberry Pi con Cloudflare Tunnel, sin coste de servidor. La PWA y la accesibilidad pasan a la 30 |
| 19 | Cierre del MVP | **Siguiente** | Cambiar la contraseña, exportar y borrar la cuenta; el admin lista reservas y cancela clases; editar las notas y la capacidad de una clase |
| 20 | Avisos y calendario | Pendiente | Correos de reserva y cancelación al alumno y aviso a Marcos; `.ics`; cancelar un día entero por lluvia |
| 21 | Lista de espera | Pendiente | Una plaza liberada se reserva sola al primero de la lista |
| 22 | Clases que se repiten | Pendiente | La misma clase durante varias semanas de una vez |
| 23 | Niveles y ficha del alumno | Pendiente | Niveles de Marcos, clases por nivel, ficha con objetivos, notas privadas e historial |
| 24 | Mensajes | Pendiente | A un grupo, un nivel o todos, por correo y en un tablón |
| 25 | Resumen del mes | Pendiente | Clases dadas, ocupación, asistencia y alumnos activos |
| 26 | Familias | Pendiente | Un adulto gestiona los perfiles de sus hijos y reserva por ellos |
| 27 | Bonos | Pendiente | Tipos de bono de Marcos; el pago se apunta, no se cobra en la app |
| 28 | Mensualidades | Pendiente | Cuota por grupo fijo y registro de quién ha pagado |
| 29 | Progreso del alumno | Pendiente | Objetivos por nivel que Marcos marca como conseguidos |
| 30 | PWA y accesibilidad | Pendiente | Instalable, sin conexión sin datos personales, axe sin fallos graves y WebKit en los flujos del alumno |
| 18 | Revisión final de arquitectura | Pendiente | Informe con severidades; nada se aplica sin aprobación. Va la última |

Las reglas de producto de las fases 19–29 ya decididas están en `01-analisis-funcional.md` §18.
Aparcado hasta que Daniel diga: web pública y clase de prueba, idiomas, pago online y facturas,
varios profesores o pistas, torneos, alquiler de pista y valoraciones.

## Decisiones de proceso resueltas

- **Sin rama `develop`.** Una sola rama larga (`main`) y ramas cortas por fase: con una persona
  y un PR por fase, una segunda rama larga sólo duplica fusiones. Los PR encadenados ya dejaron
  una vez `main` parado en la Fase 10.
- **Tres roles** (`ADMIN`, `TEACHER`, `STUDENT`), no dos: el modelo, la API y reglas como la
  cancelación sin ventana dependen de `ADMIN`.
- **La integración continua se adelantó** a la 5.1, para que todo lo posterior naciera cubierto.
- **El frontend no espera al final**: empieza tras el calendario, que es lo primero que permite a
  un alumno ver qué reservar.
