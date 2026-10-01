# Tennis Platform — Análisis funcional del MVP

## 1. Objetivo

Tennis Platform será una aplicación web/PWA para que un profesor de tenis gestione sus alumnos, disponibilidad, clases, calendario y reservas.

El MVP tendrá un único profesor, permitirá clases individuales y grupales y confirmará las reservas automáticamente.

## 2. Actores y roles

### ADMIN

Rol interno para la consola administrativa. Puede gestionar configuración global, usuarios y el límite de alumnos.

### TEACHER

Único profesor del MVP. Gestiona alumnos, disponibilidad, clases, reservas y asistencia.

### STUDENT

Usuario registrado que puede consultar su perfil, clases disponibles, reservas y próximas clases. Solo puede reservar cuando está gestionado por el profesor.

## 3. Registro y acceso

- El registro de alumnos es público.
- El registro público no autoriza automáticamente a reservar.
- El email debe verificarse.
- Se debe poder iniciar sesión, cerrar sesión y recuperar la contraseña.
- El profesor se creará mediante bootstrap inicial o desde la consola de administración.
- El rol ADMIN no podrá registrarse públicamente.

## 4. Perfil de usuario

El perfil debe permitir gestionar:

- Nombre.
- Apellidos.
- Teléfono.
- DNI.
- Dirección.
- Email.
- Zona horaria.

El cambio de email queda fuera del MVP inicial. Cualquier usuario puede cambiar su contraseña y
descargar sus datos. El alumno puede borrar su cuenta (§18); el profesor y el admin, que crea el
bootstrap, no. Las notas de las clases son texto libre del profesor y el borrado no las toca, así que
no deben llevar datos de alumnos.

## 5. Gestión de alumnos

El profesor podrá:

- Buscar alumnos registrados.
- Gestionarlos mediante email o DNI.
- Consultar sus datos necesarios para la actividad.
- Ver sus reservas.
- Desactivarlos.

Un alumno desactivado:

- No podrá reservar nuevas clases.
- Perderá sus reservas futuras activas.
- Dejará de ocupar plazas.
- Mantendrá, cuando sea necesario, una referencia histórica mínima.

## 6. Límite de alumnos

El administrador podrá configurar un límite global.

- Siempre es un número positivo: un límite alto hace lo mismo que "sin límite" sin un caso
  especial en cada comprobación (Fase 12).
- El valor se modifica desde la consola. Bajarlo por debajo de los alumnos ya gestionados no
  da de baja a nadie.
- Cuando se alcance el límite, no se podrán gestionar nuevos alumnos.
- El MVP no aplica todavía límites por profesor porque solo existe uno.

## 7. Disponibilidad

El profesor podrá definir reglas semanales y excepciones.

Ejemplo:

- Lunes de 09:00 a 13:00.
- Miércoles de 16:00 a 20:00.

Reglas:

- La duración mínima es 30 minutos.
- Los intervalos deben usar múltiplos de 30 minutos.
- No se permiten franjas que crucen medianoche.
- No se permiten franjas solapadas.
- Las excepciones pueden bloquear o añadir disponibilidad.
- Las excepciones tienen prioridad sobre la regla semanal.
- La disponibilidad se utiliza como restricción para crear clases.
- El profesor puede crear una clase fuera de disponibilidad mediante un override explícito.
- Los alumnos nunca pueden crear clases.

## 8. Clases

El profesor puede crear:

### Clase individual

- Capacidad fija de 1.
- Una única reserva activa.

### Clase grupal

- Capacidad configurable superior a 1.
- Puede comenzar con un solo participante.
- Se cierra cuando alcanza la capacidad.

Datos principales:

- Tipo.
- Fecha.
- Hora de inicio.
- Hora de finalización.
- Duración.
- Capacidad.
- Notas.
- Estado.
- Zona horaria de planificación.

Las clases con reservas no deberían cambiar de horario o duración. La operación recomendada es cancelar y crear otra.

## 9. Reservas

El alumno podrá reservar si:

- Está autenticado.
- Tiene email verificado.
- Está activo.
- Está gestionado por el profesor.
- La clase está abierta.
- Existe capacidad.
- No tiene una reserva activa para la clase.
- No tiene otra clase solapada.

La reserva queda confirmada inmediatamente.

## 10. Cancelaciones

El alumno puede cancelar su reserva hasta 24 horas antes del inicio. **El profesor puede cancelar
su clase, o una reserva en ella, en cualquier momento antes de que empiece** (corregido en las
fases 8 y 9): la ventana protege al profesor de un hueco que ya no puede llenar, y eso no es
motivo para atarlo a él. El administrador tampoco está sujeto a la ventana.

No existen penalizaciones en el MVP.

Cuando se cancela una clase:

- La clase deja de estar disponible.
- Sus reservas activas pasan a canceladas por el profesor.
- Se conserva la información histórica mínima.

Cuando se cancela una reserva:

- Se libera la plaza.
- El alumno puede volver a reservar si el producto lo permite y queda disponibilidad.
- La reserva original no se reactiva.

## 11. Asistencia

El profesor podrá marcar cada reserva como:

- Pendiente.
- Asistió.
- No asistió.

La asistencia se marcará una vez iniciada o finalizada la clase.

## 12. Estados

### Reserva

- `CONFIRMED`.
- `CANCELLED_BY_STUDENT`.
- `CANCELLED_BY_TEACHER`.
- `CANCELLED_BY_ADMIN`.

La asistencia se modela separadamente como `PENDING`, `ATTENDED` o `NO_SHOW`.

### Clase

- `OPEN`.
- `FULL`, calculado según reservas y capacidad.
- `CANCELLED`.
- `COMPLETED`.

## 13. Calendario y zonas horarias

- Los instantes se almacenan en UTC.
- Las reglas recurrentes se interpretan en la zona horaria del profesor.
- Cada usuario visualiza las fechas en su zona local.
- El calendario debe mostrar la zona cuando pueda existir ambigüedad.
- Los cambios de horario de verano e invierno se resuelven con una base IANA.
- Las horas locales inexistentes no podrán seleccionarse.
- Las horas ambiguas deben mostrar el offset o una indicación equivalente.
- No se permiten clases que crucen medianoche.

## 14. Casos límite

- Dos alumnos reservan simultáneamente la última plaza.
- El mismo alumno envía dos peticiones simultáneas.
- Se cancela una clase con reservas.
- Se desactiva un alumno con reservas futuras.
- Se intenta reservar una clase llena, pasada o cancelada.
- Se intenta crear una clase solapada.
- Se reduce una capacidad por debajo de las reservas existentes.
- Se modifica la disponibilidad cuando ya existen clases.
- Se crea una clase durante un cambio horario.

## 15. Requisitos no funcionales

- Autorización en backend por rol y propiedad del recurso.
- Contraseñas almacenadas mediante hash seguro.
- Tokens de sesión protegidos y no almacenados en `localStorage`.
- Datos personales protegidos y fuera de logs.
- API con errores normalizados mediante Problem Details.
- Logs estructurados y correlation ID.
- Health checks y métricas básicas.
- PWA instalable.
- Reservas y cancelaciones requieren conexión.
- Tests unitarios, integración, arquitectura, concurrencia y E2E.

## 16. Fuera del MVP

- Pagos online (los bonos y las mensualidades se apuntan, no se cobran: §18).
- Penalizaciones.
- Notificaciones push. Los avisos por correo sí entran (§18).
- Integración con Google Calendar o Apple Calendar.
- Múltiples profesores.
- Multi-tenant.
- Kafka.
- Microservicios.
- Copias de seguridad automatizadas.
- Auditoría formal.

## 17. Decisiones resueltas

- **Una sola pista.** No se modela una entidad "pista": el solapamiento se controla por el
  profesor. Si algún día hay varias, habrá que introducirla y separar las dos reglas.
- **Asociación alumno-profesor**: el profesor busca al alumno por su email exacto y lo gestiona.
- **Alta del profesor**: por bootstrap al desplegar, no por registro ni consola.
- **Zona mostrada**: la del profesor para todos, indicada en el calendario (Fase 11): una clase
  no cambia de hora porque alguien abra la aplicación de viaje.
- **El admin cancela clases o reservas sin ventana de 24 horas**, para resolver incidencias.
- **Una reserva cancelada no se reactiva**, pero el alumno puede volver a reservar si hay plaza.
- **Se puede reservar hasta el inicio de la clase.** Quien reserva con menos de 24 horas ya no
  puede cancelar; el frontend debe avisarlo antes de confirmar.
- **No se crean clases que ya hayan empezado.**
- **Email verificado para reservar.** El login lo acepta sin verificar; la reserva no.

## 18. Después del MVP: reglas decididas

Daniel las eligió el 30/09/2026, antes de las fases 19–29 (`12-metodologia-trabajo.md`). Cada
análisis de fase las desarrolla; no las vuelve a preguntar.

- **Borrar la cuenta anonimiza.** Se borran nombre, email, teléfono, DNI y dirección; las
  reservas pasadas quedan como "Alumno eliminado", para que resúmenes y bonos cuadren. Sus
  reservas futuras se cancelan.
- **Correos:** al alumno, la reserva confirmada (con `.ics`) y la cancelación de su clase o su
  reserva por el profesor o el admin; a Marcos, cada reserva y cancelación de un alumno. Sin
  recordatorio previo.
- **Lista de espera:** una plaza liberada se reserva sola al primero de la lista, que recibe el
  correo. Si no puede ir, cancela como cualquier reserva.
- **Clases que se repiten:** antes de crearlas se muestran las fechas que chocan (con otra clase
  o fuera del horario), y Marcos decide si salta esas o cambia algo.
- **Niveles:** los define Marcos, empezando por iniciación, intermedio, avanzado y competición.
  El alumno ve todas las clases, pero sólo reserva las de su nivel o las abiertas a todos.
- **Mensajes de Marcos** a un grupo, un nivel o todos: por correo y en un tablón de la app.
- **Familias:** los hijos son perfiles dentro de la cuenta de un adulto, que reserva por ellos.
  El niño no tiene email ni contraseña.
- **Bonos:** la clase se descuenta al reservar y se devuelve si se cancela a tiempo (más de
  24 horas); si no viene, se pierde. Sin saldo no se reserva. Cada tipo de bono tiene el precio
  y la caducidad (o ninguna) que ponga Marcos.
- **Mensualidades:** por grupo fijo. El alumno se apunta al grupo, paga la cuota del mes y sus
  clases del grupo quedan reservadas.
