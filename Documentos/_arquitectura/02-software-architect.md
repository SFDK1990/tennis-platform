# Tennis Platform — Documento del Software Architect

## Decisión arquitectónica

El backend será un Modular Monolith con arquitectura hexagonal por módulo.

La aplicación se desplegará inicialmente como una unidad, pero los límites de negocio estarán definidos desde el inicio para permitir una futura extracción si aparece una razón real.

## Módulos

### identity

Usuarios, autenticación, roles, verificación de email, recuperación de contraseña y ciclo de vida de cuentas.

### teacher

Perfil y configuración del profesor único.

### student

Perfil de alumno y relación de alumno gestionado por el profesor.

### availability

Reglas semanales, excepciones y evaluación de disponibilidad.

### lesson

Clases, capacidad, horarios, estados y asistencia.

### booking

Reservas, capacidad, duplicados, solapamientos y cancelaciones.

### administration

Consola administrativa y configuración global, incluido el límite de alumnos.

### calendar

Capacidad de consulta que agrega datos de disponibilidad, clases y reservas. No posee entidades de negocio propias en el MVP.

### shared

Primitivas técnicas mínimas, errores comunes, identificadores y utilidades de infraestructura. No contendrá lógica específica de negocio.

## Dependencias permitidas

- `identity` no depende de módulos de negocio.
- `teacher` puede consultar identidad.
- `student` puede consultar identidad y la capacidad pública del profesor.
- `availability` puede consultar el perfil horario del profesor.
- `lesson` puede consultar profesor y disponibilidad.
- `booking` puede consultar alumno y clase.
- `administration` puede consultar identidad, profesor y alumno.
- `calendar` solo puede utilizar interfaces públicas de consulta.

Está prohibido acceder a repositorios o entidades internas de otro módulo.

## Arquitectura hexagonal

Cada módulo tendrá:

- `domain`: entidades, objetos de valor, reglas y excepciones.
- `application`: casos de uso y puertos.
- `adapters/in`: REST controllers y otros adaptadores de entrada.
- `adapters/out`: persistencia, seguridad y servicios externos.
- `configuration`: ensamblado de dependencias.

El dominio no dependerá de Spring, JPA, REST ni PostgreSQL.

## Estructura propuesta

El proyecto se organizará por módulo y no por capas globales. La estructura inicial `auth`, `user`, `teacher`, `student`, `availability`, `lesson` y `booking` se considera válida conceptualmente, pero `auth` y `user` se consolidan en `identity`, y se añade `administration`.

## Comunicación entre módulos

Se preferirán interfaces públicas de aplicación y consultas específicas.

No se introducirá Kafka ni un bus distribuido. Los eventos internos solo se utilizarán si evitan una dependencia directa clara, por ejemplo para notificar que un alumno ha sido desactivado o que una clase ha sido cancelada.

## Calendario

El calendario se considera una vista agregada, no un agregado de dominio. Las escrituras pertenecerán a disponibilidad, lesson o booking; el calendario solo consultará sus modelos públicos.

## Por qué no microservicios

- No existe una necesidad de escalado independiente.
- Las reservas requieren consistencia transaccional fuerte.
- El dominio es pequeño y coherente.
- El equipo se beneficiará de un despliegue único.
- Los microservicios añadirían complejidad operativa sin beneficio actual.

## ADRs principales

1. Modular Monolith.
2. Arquitectura hexagonal.
3. Módulos por capacidad de negocio.
4. Consolidación de `auth` y `user` en `identity`.
5. PostgreSQL como fuente transaccional.
6. Liquibase para migraciones.
7. Instantes UTC y zonas IANA.
8. Reservas automáticas.
9. Control de concurrencia en base de datos.
10. Disponibilidad como restricción y override explícito del profesor.
11. Separación conceptual entre clase y reserva.
12. Rol ADMIN para configuración global.
13. Calendario como consulta agregada.
14. Sin microservicios, Kafka ni eventos externos en el MVP.
