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
| 15.5 | Experiencia y diseño | **Siguiente** | Maqueta aprobada por Daniel antes de implementar; identidad visual, "Hoy" del profesor, próxima clase del alumno y pantallas vacías que guían. Sólo frontend, con los E2E como red |
| 16 | Observabilidad | Pendiente | Logs estructurados, métricas de reservas y conflictos, correlation id de punta a punta |
| 17 | Endurecimiento y despliegue | Pendiente | PWA completa, accesibilidad, proveedor elegido y política de backups |
| 18 | Revisión final de arquitectura | Pendiente | Informe con severidades; nada se aplica sin aprobación |

## Decisiones de proceso resueltas

- **Tres roles** (`ADMIN`, `TEACHER`, `STUDENT`), no dos: el modelo, la API y reglas como la
  cancelación sin ventana dependen de `ADMIN`.
- **La integración continua se adelantó** a la 5.1, para que todo lo posterior naciera cubierto.
- **El frontend no espera al final**: empieza tras el calendario, que es lo primero que permite a
  un alumno ver qué reservar.
