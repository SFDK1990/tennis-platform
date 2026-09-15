# Tennis Platform — Documento del Senior Database Engineer

## Base de datos

PostgreSQL será la fuente transaccional del MVP.

Las migraciones se gestionarán exclusivamente con Liquibase.

## Principios

- UUID como identificadores públicos.
- Timestamps en UTC.
- Zonas horarias IANA como datos de configuración.
- Nombres de tablas y columnas consistentes.
- Constraints de base de datos para invariantes críticas.
- No usar entidades JPA como diseño de modelo sin revisar el dominio.
- No borrar históricamente reservas necesarias para mantener consistencia.

## Entidades principales

### users

Identidad, email, rol, estado, hash de contraseña y timestamps.

### teacher_profiles

Perfil del profesor y zona horaria.

### student_profiles

Nombre, teléfono, DNI, dirección y datos de perfil.

### teacher_students

Relación entre el profesor único y los alumnos gestionados.

### email_verifications

Tokens de verificación con caducidad y uso único.

### password_reset_tokens

Tokens de recuperación con caducidad y uso único.

### weekly_availability_rules

Día de semana, hora inicial, hora final y vigencia.

### availability_exceptions

Bloqueos o disponibilidad extraordinaria por fecha.

### lessons

Tipo, fecha/hora, duración, capacidad, estado, notas y versión.

### bookings

Alumno, clase, estado, timestamps de reserva y cancelación.

### attendance

Estado de asistencia por reserva, si se decide separarlo del propio booking.

### platform_configuration

Límite global de alumnos y parámetros configurables.

## Constraints necesarias

- Email único, normalizado.
- Un usuario con un único rol válido.
- Una relación alumno-profesor única.
- Capacidad positiva.
- Clase individual con capacidad uno.
- Duración positiva y múltiplo de 30.
- Inicio anterior al fin.
- Reserva única por alumno y clase.
- No reservas activas para usuarios desactivados.
- No superar capacidad.
- No solapamiento de clases del profesor.

Los solapamientos de rangos deberían protegerse con PostgreSQL además de la validación de aplicación.

## Índices

- Email normalizado.
- Estado de usuario.
- Fecha de clase.
- Estado de clase.
- Profesor y rango temporal.
- Alumno y rango temporal.
- Reserva por clase.
- Reserva por alumno.
- Tokens por hash y expiración.

## Liquibase

Los cambios se dividirán por contexto:

1. Identity.
2. Teacher.
3. Student.
4. Availability.
5. Lesson.
6. Booking.
7. Administration.
8. Índices y constraints adicionales.

Los changelogs ejecutados no se modificarán. Las correcciones se realizarán mediante nuevos changesets.

## Concurrencia

La reserva de una clase deberá ejecutarse dentro de una transacción y bloquear el recurso de capacidad.

La base de datos deberá impedir:

- Duplicados.
- Sobrepasar capacidad.
- Solapamiento de clases del profesor.

La lógica de solapamiento del alumno deberá combinar consulta transaccional y control de concurrencia.

## Datos personales

DNI, dirección, teléfono y email requieren:

- Acceso restringido.
- No exposición en listados innecesarios.
- Exportación controlada.
- Eliminación o anonimización según política legal.
- No aparecer en logs.

## Riesgo aceptado

El MVP no tendrá política automatizada de copias de seguridad. Esto debe figurar como riesgo crítico antes de producción real.
