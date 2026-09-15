# Tennis Platform — Documento del DevOps Engineer

## Objetivo operativo

Mantener un flujo sencillo y reproducible para desarrollo local, integración continua y futuros entornos de prueba o producción.

## Componentes

- Frontend Next.js.
- Backend Spring Boot.
- PostgreSQL.
- Liquibase.
- Docker.
- Docker Compose.
- Git.
- GitHub.
- GitHub Actions.

## Repositorio

Se recomienda un monorepo:

- `frontend/`.
- `backend/`.
- `docs/`.
- `docker/`.
- `.github/workflows/`.

Ventajas:

- Cambios frontend/backend coordinados.
- Una única pipeline.
- Contratos REST versionados junto al producto.
- Desarrollo local sencillo.

## Entornos

### Local

- Docker Compose.
- PostgreSQL local.
- Backend con perfil local.
- Frontend con variables locales.

### Test

- Base de datos efímera.
- Liquibase ejecutado desde cero.
- Tests de integración.

### Staging

- Configuración separada.
- Datos no productivos.
- Validación manual y E2E.

### Producción futura

- Secretos gestionados fuera de Git.
- PostgreSQL administrado.
- Backups obligatorios antes de uso real.
- TLS.
- Monitorización.

## GitHub Actions

La pipeline deberá comprobar:

- Formato y compilación backend.
- Tests backend.
- Tests de arquitectura.
- Tests de migraciones.
- Lint frontend.
- Type checking.
- Tests frontend.
- Tests E2E.
- Construcción de imágenes.

## Configuración

Las variables sensibles serán externas:

- Base de datos.
- Secretos JWT.
- Orígenes CORS.
- Configuración de email.
- Límites del producto.

No se almacenarán secretos en repositorio.

## Liquibase en despliegue

Las migraciones deben ejecutarse de forma controlada antes de iniciar una nueva versión compatible del backend.

Los cambios de esquema deberán ser compatibles con despliegues graduales si se adopta esa estrategia en el futuro.

## Observabilidad operativa

- Health checks.
- Logs estructurados.
- Correlation ID.
- Métricas HTTP.
- Métricas de reservas.
- Alertas futuras sobre errores y base de datos.

## No se incorpora aún

- Kubernetes.
- Kafka.
- Service mesh.
- Despliegues multi-región.
- Escalado horizontal avanzado.
- Infraestructura cloud definitiva.

## Riesgos

- Docker debe estar disponible para validar el entorno completo.
- No existe política de backups en el MVP.
- El proveedor de producción se deja deliberadamente sin decidir durante el desarrollo del MVP: local, test y staging básico se resuelven con Docker Compose, y la elección de proveedor (VPS, PaaS gestionado, etc.) se pospone hasta que haya que exponer el sistema con usuarios reales.
- Las reglas de seguridad y TLS deben cerrarse antes de exponer el sistema públicamente.
