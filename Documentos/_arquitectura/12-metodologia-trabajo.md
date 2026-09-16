# Tennis Platform — Metodología de trabajo

## Propósito

Este documento define **cómo** se construye Tennis Platform: el orden de las fases, qué se puede hacer en cada una y qué debe entregarse antes de pasar a la siguiente. El resto de documentos de esta carpeta definen *qué* se construye; este define el proceso.

Va dirigido tanto a Daniel como a cualquier agente de programación que trabaje sobre el repositorio.

## Origen

La metodología procede de una sesión de diseño previa, conservada como exportación en PDF (`tenisAPP.pdf`, 27 páginas). Este documento la recoge en texto para que quede versionada junto al código y sea consultable sin depender de un PDF de capturas de pantalla.

La sección "Reglas adicionales" recoge añadidos posteriores, surgidos de la experiencia real de ejecutar la Fase 4 y de trabajar con un agente que dispone de terminal. Fueron aprobados el 16 de septiembre de 2026 y tienen el mismo rango que el resto.

## Regla central

> **No avanzar de fase hasta haber validado la anterior.**

El agente no encadena fases por iniciativa propia. Al terminar una fase se detiene, entrega el informe de cierre y espera revisión.

## Principios

Se aplican SOLID, Clean Code, separación de responsabilidades, modularización por dominio cuando aporte valor, inversión de dependencias, diseño API-first, seguridad por defecto, testabilidad, observabilidad y mantenibilidad.

Tres restricciones que acotan lo anterior:

- No introducir complejidad innecesaria.
- No usar patrones simplemente por usarlos.
- Cada decisión arquitectónica debe tener una razón explicable.

Kafka y la IA quedan fuera del MVP. Kafka se incorporará cuando exista una necesidad arquitectónica real (por ejemplo, eventos de dominio que alimenten email, calendario o analítica), nunca por aparentar madurez técnica.

## Reglas de trabajo

Antes de escribir código en cualquier fase:

1. Analizar los requisitos.
2. Identificar ambigüedades.
3. Proponer la arquitectura.
4. Explicar las decisiones importantes.
5. Definir la estructura de carpetas.
6. Definir el modelo de datos.
7. Definir los contratos de API.
8. Definir la estrategia de testing.
9. Definir los criterios de aceptación.

Después implementar.

Si se detecta una mala decisión arquitectónica previa, **no debe mantenerse por compatibilidad**: hay que señalarla y proponer una alternativa.

Sobre el nivel de detalle: al cerrar cada bloque grande de implementación se entrega un **resumen de las decisiones tomadas y su porqué**, no un volcado de código sin explicar, pero tampoco una narración de cada archivo mientras se trabaja.

## Informe de cierre de fase

Al finalizar cada fase se entrega:

- Cambios realizados.
- Archivos creados o modificados.
- Decisiones tomadas.
- Tests ejecutados.
- Problemas encontrados.
- Riesgos pendientes.
- Cómo ejecutar el proyecto.
- Qué debería revisarse antes de continuar.

## Secuencia de fases

| # | Fase | Contenido | Estado |
|---|------|-----------|--------|
| 1 | Análisis funcional | User stories, requisitos funcionales y no funcionales, roles, casos de uso, reglas de negocio, casos límite. Sin código. | Completada |
| 2 | Arquitectura | Módulos, responsabilidades, dependencias permitidas, capas, estructura de paquetes, ADRs. Sin código. | Completada |
| 3 | Modelo de datos | Entidades, relaciones, restricciones, índices, diagrama ER y borrador de DDL. | Completada |
| 4 | Skeleton del backend | Proyecto Maven compilable, estructura modular, perfiles, Docker, health endpoint, manejo de errores, logging y tests mínimos. Sin lógica de negocio. | Completada |
| 5 | Seguridad y autenticación | Registro, verificación por email, login, JWT, refresh tokens rotativos, autorización por rol y por pertenencia. | Completada (los dos criterios abiertos se cerraron en la 5.1) |
| 5.1 | Integración continua | Pipeline de GitHub Actions: build, tests unitarios, tests de integración, análisis estático, comprobación de dependencias y construcción de imagen. Adelantada desde la fase 15. | Completada (ver `14-fase5.1-integracion-continua.md`) |
| 6 | Perfiles y gestión de usuarios | Perfiles de profesor y alumno, alta y asociación de alumnos, activación y desactivación. | Siguiente |
| 7 | Disponibilidad | Reglas semanales de disponibilidad del profesor y excepciones. | Pendiente |
| 8 | Clases | Creación, consulta, modificación y cancelación de clases, con validación de solapamientos. | Pendiente |
| 9 | Reservas | Reserva y cancelación, capacidad, duplicados, solapamientos del alumno y concurrencia. | Pendiente |
| 10 | Revisión de API | Revisión REST completa: naming, verbos, códigos, paginación, errores, idempotencia, versionado. OpenAPI. | Pendiente |
| 11 | Frontend | Next.js, React, TypeScript, Tailwind. Estados de carga, vacío y error. | Pendiente |
| 12 | Cobertura y E2E | Pirámide de testing completa y flujos E2E críticos. | Pendiente |
| 13 | Auditoría de seguridad | OWASP Top 10, escalada de privilegios, acceso a recursos ajenos, secretos, dependencias. | Pendiente |
| 14 | Observabilidad | Logs estructurados, métricas, trazas y correlation IDs, ejecutables en local. | Pendiente |
| 15 | Empaquetado de despliegue | Imágenes de producción y ajustes finales de Docker. El pipeline de integración continua se adelantó a la fase 5.1. | Pendiente |
| 16 | Revisión final de arquitectura | Informe con severidades CRITICAL / HIGH / MEDIUM / LOW y preguntas de escalabilidad. Sin aplicar cambios sin aprobación. | Pendiente |

## Reglas adicionales

Aprobadas el 16 de septiembre de 2026. Surgen de ejecutar la Fase 4 y de trabajar con un agente que dispone de terminal, no solo de chat. Tienen el mismo rango que las reglas de las secciones anteriores.

### 1. La evidencia se pega, no se afirma

Una fase no se da por terminada sin la salida real del comando. Un build en verde no es evidencia suficiente: debe mostrarse el recuento de tests ejecutados, fallados y **saltados**.

Motivo: durante la Fase 4 la suite reportó `BUILD SUCCESS` mientras cinco de los ocho tests se saltaban en silencio, porque Testcontainers no lograba conectar con Docker. El resultado parecía correcto y no lo era.

### 2. Cada fase cierra con un commit

La metodología original no menciona el control de versiones en ningún punto. Las cuatro primeras fases se completaron sin un solo commit. Cada fase debe terminar con su propio commit, cuyo mensaje describa la fase y su verificación.

### 3. La integración continua se adelanta

La integración continua estaba en la fase 15. Situarla al final implica descubrir tarde si el proyecto es reproducible fuera de la máquina de desarrollo. Pasa a ser la fase 5.1, justo después de autenticación, de forma que todo el trabajo posterior nazca ya cubierto por el pipeline.

### 4. El testing no es una fase

Las fases 6 a 9 ya exigen tests unitarios y de integración en cada módulo, lo que contradice que el testing sea una fase propia al final. La fase 12 se reinterpreta como *cobertura y E2E* —cerrar huecos y cubrir los flujos completos—, no como el momento en que empiezan a escribirse tests.

### 5. Criterios de aceptación medibles

Cada fase debe declarar su criterio de salida en términos verificables. Por ejemplo, para la Fase 9: *"existe un test que demuestra que dos reservas simultáneas del último hueco libre no producen sobreventa"*, en lugar de *"implementa el sistema de reservas"*.

## Decisiones resueltas

### Número de roles

El documento de origen definía **dos roles** (`TEACHER` y `STUDENT`), mientras que el resto de la documentación de arquitectura, el modelo de datos y el contrato de API trabajaban con **tres** (`ADMIN`, `TEACHER`, `STUDENT`).

Resuelto el 16 de septiembre de 2026 a favor de **tres roles**. `ADMIN` se mantiene porque ya está incorporado al modelo de datos, al diagrama ER y al contrato de API, y porque hay reglas de negocio que dependen de él —cancelar fuera de la ventana de 24 horas y desactivar alumnos—. Donde el documento de origen mencione dos roles, prevalece esta decisión.
