# Tennis Platform — Documento del QA/Test Engineer

## Objetivo de calidad

Garantizar que el sistema respeta las reglas de reservas, horarios, capacidad, autorización y zonas horarias, especialmente bajo concurrencia.

## Pirámide de testing

### Unitario

Para reglas puras:

- Duraciones.
- Disponibilidad.
- Excepciones.
- Solapamientos.
- Capacidad.
- Estados.
- Cancelaciones.
- Zonas horarias.

### Aplicación

Para casos de uso:

- Registro.
- Login.
- Gestión de alumno.
- Crear clase.
- Reservar.
- Cancelar.
- Marcar asistencia.
- Configurar límite.

### Integración

Con PostgreSQL real:

- Liquibase.
- Constraints.
- Índices.
- Locking.
- Transacciones.
- Recuperación de errores.

### API

- Códigos HTTP.
- DTOs.
- Validación.
- Autenticación.
- Autorización.
- Problem Details.

### E2E

- Registro.
- Verificación.
- Login.
- Gestión de alumno.
- Disponibilidad.
- Creación de clase.
- Reserva.
- Cancelación.
- Asistencia.
- Administración.

## Casos críticos de prueba

### Capacidad

- Reservar la primera plaza.
- Reservar la última plaza.
- Intentar reservar una clase llena.
- Cancelar y liberar una plaza.
- Dos usuarios reservando simultáneamente la última plaza.

### Solapamientos

- Dos clases del profesor solapadas.
- Dos reservas solapadas del mismo alumno.
- Clases contiguas sin falso solapamiento.
- Solapamiento exacto de inicio y fin.

### Cancelaciones

- Cancelar exactamente 24 horas antes.
- Cancelar por debajo de 24 horas.
- Cancelar una reserva ya cancelada.
- Cancelar una clase con varios alumnos.
- Desactivar alumno con reservas futuras.

### Zonas horarias

- Zona del profesor distinta de la del alumno.
- Cambio a horario de verano.
- Cambio a horario de invierno.
- Hora local inexistente.
- Hora local ambigua.
- Clase que no cruza medianoche.

### Seguridad

- Alumno accediendo a otra reserva.
- Alumno intentando gestionar alumnos.
- Profesor intentando acceder a administración.
- Usuario sin email verificado intentando reservar.
- Token expirado.
- Token de recuperación reutilizado.

## Tests de arquitectura

Se validará que:

- El dominio no depende de Spring.
- Los controllers no acceden directamente a repositorios.
- Los módulos no usan entidades JPA de otros módulos.
- No existen ciclos de dependencia.
- `calendar` no escribe directamente en dominios ajenos.

## Criterios de salida

No se considerará listo un incremento si:

- Falla un test de concurrencia.
- Puede superarse la capacidad.
- Puede duplicarse una reserva.
- Se permite acceso horizontal entre alumnos.
- Una migración no puede ejecutarse desde una base vacía.
- Existen errores no controlados en la API.

## Datos de prueba

Se necesitarán fixtures para:

- Administrador.
- Profesor.
- Alumnos gestionados y no gestionados.
- Clases individuales.
- Clases grupales abiertas y llenas.
- Reservas confirmadas y canceladas.
- Cambios de zona horaria.

## Automatización

La CI deberá ejecutar tests unitarios, integración, arquitectura, frontend y E2E antes de aceptar cambios principales.
