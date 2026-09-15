# Tennis Platform — Documento del Product Architect

## Objetivo

Definir un MVP realista para que un único profesor gestione alumnos, disponibilidad, calendario, clases y reservas desde una aplicación web/PWA.

## Recursos físicos

El MVP asume una única pista/cancha para todas las clases del profesor. No se modela una entidad "pista" ni disponibilidad por recurso físico: el solapamiento de clases se controla exclusivamente por el profesor, como ya reflejan las reglas de negocio. Si en el futuro aparecen varias pistas, será necesario introducir esa entidad y separar las reglas de solapamiento del profesor de las del recurso físico.

## Alcance del MVP

- Registro público de usuarios.
- Verificación de email.
- Login y recuperación de contraseña.
- Roles `ADMIN`, `TEACHER` y `STUDENT`.
- Perfil con nombre, teléfono, DNI, dirección y email.
- Gestión de alumnos por el profesor.
- Límite configurable de alumnos desde la consola de administración.
- Disponibilidad semanal y excepciones por fecha.
- Calendario en hora local.
- Clases individuales y grupales.
- Reservas automáticas.
- Cancelaciones hasta 24 horas antes.
- Marcado de asistencia.
- Visualización de próximas clases.
- Exportación y eliminación de datos personales.
- PWA instalable con soporte offline limitado a lectura.

## Actores

### ADMIN

Usuario interno encargado de la configuración global y gestión operativa.

### TEACHER

Único profesor del MVP. Gestiona alumnos, disponibilidad, clases y reservas.

### STUDENT

Puede consultar su perfil, clases disponibles, reservas y próximas clases. Solo puede reservar si está gestionado por el profesor.

## Flujo de alta

1. El usuario se registra públicamente como alumno.
2. Verifica su email.
3. Completa sus datos personales.
4. El profesor lo busca y lo gestiona.
5. El alumno puede reservar clases.

El registro público no equivale a autorización para reservar.

## Reglas de negocio principales

- Una clase individual tiene capacidad uno.
- Una clase grupal tiene capacidad configurable superior a uno.
- Una clase grupal puede comenzar con un solo alumno.
- La duración mínima es de 30 minutos y debe ser múltiplo de 30.
- No se permiten clases que crucen medianoche.
- No puede haber solapamiento de clases del profesor.
- Un alumno no puede tener dos clases solapadas.
- Una reserva confirmada ocupa una plaza.
- Solo una petición gana la última plaza en una reserva concurrente.
- Las cancelaciones se permiten hasta 24 horas antes.
- El ADMIN puede cancelar clases o reservas sin respetar la ventana de 24 horas.
- No existen penalizaciones en el MVP.
- Desactivar un alumno cancela sus reservas futuras activas.
- El profesor puede crear clases fuera de disponibilidad mediante una acción explícita.
- Un alumno se convierte en "gestionado" cuando el profesor lo busca por email y lo asocia explícitamente.

## Estados

### Reserva

`CONFIRMED`, `CANCELLED_BY_STUDENT`, `CANCELLED_BY_TEACHER`, `ATTENDED`, `NO_SHOW`.

### Clase

`OPEN`, `FULL`, `CANCELLED`, `COMPLETED`.

## Criterios de aceptación

- El alumno puede registrarse y verificar su email.
- El profesor puede gestionarlo antes de permitir reservas.
- El profesor puede crear clases individuales y grupales.
- Las reservas se confirman automáticamente.
- El sistema impide sobrepasar capacidad y solapamientos.
- El profesor puede cancelar clases y marcar asistencia.
- La información se muestra en hora local.
- El administrador puede configurar el límite de alumnos.

## Fuera del MVP

- Pagos.
- Penalizaciones.
- Notificaciones externas.
- Calendarios externos.
- Varios profesores.
- Multi-tenant.
- Kafka.
- Microservicios.
- Copias de seguridad automatizadas.
- Auditoría formal.

## Riesgos de producto

- No disponer inicialmente de copias de seguridad.
- Almacenar DNI y dirección.
- Necesidad futura de ampliar el modelo a varios profesores.
- Posible necesidad de excepciones a la ventana de cancelación de 24 horas.

## Decisiones resueltas (antes pendientes)

- **Asociación alumno-profesor**: el profesor busca al alumno por email (ya único y verificado en el registro) y lo asocia manualmente como gestionado.
- **Alta del profesor**: se crea mediante bootstrap (seed/migración con credenciales iniciales) en el despliegue inicial, sin depender de un ADMIN previo.
- **Zona horaria mostrada al alumno**: el calendario del alumno muestra su hora local y, junto a ella, la zona horaria del profesor, para evitar confusión cuando ambos están en husos distintos.
- **Cancelación por ADMIN**: el ADMIN puede cancelar cualquier clase o reserva sin respetar la ventana de 24 horas, para resolver incidencias operativas (p. ej. enfermedad del profesor o errores).
