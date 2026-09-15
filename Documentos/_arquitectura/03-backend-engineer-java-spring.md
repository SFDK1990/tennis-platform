# Tennis Platform — Documento del Senior Backend Engineer

## Stack backend

- Java 21.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- Hibernate.
- PostgreSQL.
- Liquibase.
- REST API.
- Bean Validation.
- Actuator.
- Envío de email transaccional (verificación de cuenta, recuperación de contraseña) vía SMTP genérico (`spring-boot-starter-mail`), configurable por entorno (Mailtrap u otro SMTP de pruebas en local/test, SMTP real en staging/producción).

## Responsabilidades técnicas

El backend debe garantizar que todas las reglas críticas se cumplan aunque el frontend sea manipulado o existan peticiones concurrentes.

## Capas por módulo

### Dominio

Contendrá el modelo y las reglas puras:

- `User`.
- `TeacherProfile`.
- `StudentProfile`.
- `AvailabilityRule`.
- `AvailabilityException`.
- `Lesson`.
- `Booking`.

### Aplicación

Contendrá casos de uso explícitos:

- Registrar usuario.
- Verificar email.
- Autenticar usuario.
- Gestionar alumno.
- Configurar disponibilidad.
- Crear clase.
- Reservar clase.
- Cancelar reserva.
- Cancelar clase.
- Marcar asistencia.
- Configurar límite de alumnos.

### Adaptadores de entrada

- REST controllers.
- DTOs de entrada y salida.
- Conversión HTTP a comandos.
- Resolución del usuario autenticado.

### Adaptadores de salida

- Repositorios JPA.
- Hash de contraseñas.
- Tokens.
- Reloj.
- Persistencia de sesiones.

## REST API principal

### Authentication

- Registro público.
- Login.
- Logout.
- Refresh.
- Verificación de email.
- Recuperación de contraseña.

### Usuarios

- Consulta y modificación del propio perfil.
- Exportación de datos.
- Solicitud de eliminación.

### Profesor y alumnos

- Perfil del profesor.
- Lista de alumnos.
- Asociación y desactivación.

### Disponibilidad

- Reglas semanales.
- Excepciones.
- Bloqueos.

### Clases y reservas

- Crear clases individuales y grupales.
- Consultar calendario.
- Reservar.
- Cancelar.
- Completar.
- Marcar asistencia.

## Reglas de backend

- La duración mínima es de 30 minutos.
- La duración debe ser múltiplo de 30.
- No se permiten clases que crucen medianoche.
- Las reservas solo pertenecen a alumnos gestionados.
- Una clase individual tiene capacidad uno.
- Las clases grupales pueden comenzar con un participante.
- No se admite sobrepasar capacidad.
- No se admiten reservas duplicadas.
- No se admiten solapamientos del profesor ni del alumno.
- Las cancelaciones se validan con una ventana de 24 horas.

## Transacciones

Los límites transaccionales se situarán en la capa de aplicación.

La reserva deberá:

1. Bloquear la clase.
2. Comprobar estado y capacidad.
3. Comprobar duplicado.
4. Comprobar solapamiento del alumno.
5. Crear la reserva.
6. Confirmar la transacción.

Se utilizarán constraints de PostgreSQL junto con locking transaccional. La validación de disponibilidad de la interfaz no es suficiente.

## Seguridad backend

- Spring Security.
- Access token de corta duración.
- Refresh token rotatorio.
- Hash de refresh tokens.
- BCrypt o Argon2id.
- Roles y ownership.
- CORS restringido.
- Rate limiting para autenticación.

No se almacenarán tokens en `localStorage`.

## Errores

Se utilizará Problem Details con códigos de negocio como:

- `LESSON_FULL`.
- `LESSON_OVERLAP`.
- `BOOKING_ALREADY_EXISTS`.
- `STUDENT_NOT_MANAGED`.
- `CANCELLATION_WINDOW_EXPIRED`.
- `STUDENT_SCHEDULE_OVERLAP`.

## Persistencia

Las entidades JPA serán adaptadores y no se reutilizarán como entidades de dominio.

Los repositorios serán puertos. Las implementaciones Spring Data quedarán dentro de `adapters/out/persistence`.

## Testing backend

- Unit tests de dominio sin Spring.
- Tests de casos de uso con puertos simulados.
- Tests REST.
- Tests de integración con PostgreSQL mediante Testcontainers.
- Tests de concurrencia de última plaza.
- Tests de arquitectura con ArchUnit.
- Tests de migraciones Liquibase.
