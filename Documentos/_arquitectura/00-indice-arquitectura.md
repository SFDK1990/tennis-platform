# Tennis Platform — Índice de arquitectura

## Propósito

Este directorio contiene la definición del MVP de Tennis Platform separada por responsabilidad profesional. No contiene implementación de código.

## Decisiones consolidadas

- Frontend: Next.js, React, TypeScript, Tailwind CSS y PWA.
- Backend: Java 21, Spring Boot, Spring Security y REST API.
- Base de datos: PostgreSQL.
- Migraciones: Liquibase.
- Arquitectura backend: Modular Monolith con arquitectura hexagonal por módulo.
- Usuarios: `ADMIN`, `TEACHER` y `STUDENT`.
- MVP: un único profesor.
- Registro de alumnos: público, pero solo pueden reservar cuando están gestionados por el profesor.
- Clases: individuales y grupales.
- Reservas: automáticas y confirmadas inmediatamente.
- Cancelación: hasta 24 horas antes, sin penalizaciones.
- Zona horaria: visualización en hora local y persistencia de instantes en UTC.
- Disponibilidad: regla de validación; el profesor puede crear clases fuera de ella mediante override explícito.
- No se incorporan microservicios, Kafka, pagos ni notificaciones externas.
- Recursos físicos: una única pista/cancha en el MVP; no se modela como entidad.
- Asociación alumno-profesor: búsqueda y asociación manual por email.
- Alta del profesor: bootstrap por seed/migración, no por registro público ni consola de administración.
- Refresh token: cookie `HttpOnly` + `Secure` + `SameSite`, nunca en `localStorage` ni en el cuerpo de la respuesta.
- Email transaccional: SMTP genérico (`spring-boot-starter-mail`), sin proveedor propietario.
- Hosting de producción: decisión aplazada deliberadamente; MVP se desarrolla y prueba con Docker Compose.

## Documentos

1. [Product Architect](01-product-architect.md)
2. [Software Architect](02-software-architect.md)
3. [Senior Backend Engineer — Java/Spring](03-backend-engineer-java-spring.md)
4. [Senior Frontend Engineer — Next.js/React](04-frontend-engineer-next-react.md)
5. [Senior Database Engineer](05-database-engineer.md)
6. [DevOps Engineer](06-devops-engineer.md)
7. [QA/Test Engineer](07-qa-test-engineer.md)
8. [Security Engineer](08-security-engineer.md)
9. [Roadmap de implementación](09-roadmap-implementacion.md)
10. [Diagrama ER y borrador de DDL](10-diagrama-er.md)
11. [Contrato de API](11-contrato-api.md) (el spec OpenAPI vive en `../../TennisPlatformApp/openapi.yaml`)

## Estructura de carpetas

`Documentos/` (esta carpeta, solo `.md`) y `TennisPlatformApp/` (el proyecto de código: `backend/`, futuramente `frontend/`, `openapi.yaml`, `CLAUDE.md`, `docker-compose.yml`) son carpetas hermanas dentro de `Tennis Platform/`. Ningún artefacto de código o de herramienta (specs, YAML, Dockerfiles) debe añadirse dentro de `Documentos/`.

## Estado

Documentación de arquitectura aprobada para iniciar el diseño técnico detallado. La implementación de código queda fuera de estos documentos.
