# Fase 12 — Análisis: la administración (`administration`)

Criterio de salida (`12-metodologia-trabajo.md`): sólo `ADMIN` accede; ajusta el límite de
alumnos y el estado de cualquier usuario. Como toda fase desde la 11, trae su pantalla.

## Punto de partida

- `platform` ya guarda la configuración (`platform_configuration`, una fila): `student_limit` y
  `max_group_capacity`, ambos obligatorios y positivos, más `updated_at` y `updated_by`. Sólo
  se leen; nada los cambia.
- `identity` ya sabe desactivar una cuenta (`User.disable`), y `LoginService`,
  `RefreshSessionService` y la recuperación de contraseña rechazan a un desactivado.
  `RefreshTokens.revokeAllForUser` existe y su comentario ya lo destina a desactivar.
- `administration` es un módulo vacío. Puede depender de `identity`, `teacher`, `student` y
  `platform`, no de `booking`.
- **No existe ningún admin**: nada lo crea.
- El contrato tiene `/admin/configuration`, `/admin/configuration/student-limit`, `/admin/users`
  y `/admin/users/{id}/status`, sin implementar.

## Decisiones propuestas

1. **El primer admin lo crea el bootstrap**, como al profesor: `ADMIN_EMAIL` y
   `ADMIN_PASSWORD` en el entorno. El registro público nunca crea un admin (`01` §3), y un admin
   creado a mano en la base es justo lo que no se puede repetir en un despliegue.
2. **Una sola escritura de la configuración**: `PATCH /admin/configuration` con `studentLimit`
   y `maxGroupCapacity`, los dos opcionales. Sustituye a `/admin/configuration/student-limit`:
   la capacidad de grupo también es configuración, y un endpoint por campo multiplicaría el
   contrato sin ganar nada. Se registran `updatedAt` y `updatedBy`.
3. **Bajar el límite por debajo de los alumnos ya gestionados se permite**: no echa a nadie,
   sólo impide gestionar nuevos hasta volver a estar por debajo. La pantalla lo dice. Igual con
   la capacidad de grupo: las clases ya creadas conservan la suya.
4. **El límite es un número, no "sin límite"**. `01` §6 decía que `NULL` significaba sin
   límite; la tabla lo hizo obligatorio en la Fase 6. Se corrige `01` en vez de la tabla: un
   límite alto hace lo mismo sin un caso especial en cada comprobación.
5. **El admin cambia el estado de alumnos, no del profesor ni de otros admins.** Desactivar al
   único profesor apagaría la plataforma, y un admin que desactiva a otro (o a sí mismo) puede
   quedarse sin nadie que lo deshaga. Se responde `403 ADMIN_TARGET_NOT_ALLOWED`.
6. **Desactivar cierra las sesiones** (`revokeAllForUser`). El access token vivo dura como
   mucho sus 15 minutos: comprobar el estado en cada petición costaría una consulta por
   llamada para acortar una ventana ya corta.
7. **Desactivar a un alumno también deja de gestionarlo**, y eso cancela sus reservas futuras,
   por el camino que ya existe (`ManageStudent.stopManaging`). Sin esto, sus plazas seguirían
   ocupadas por alguien que no puede entrar. Las reservas quedan `CANCELLED_BY_ADMIN`, no
   `BY_TEACHER`: el motivo tiene que decir quién decidió.
8. **Reactivar devuelve la cuenta a como estaba**: `ACTIVE` si había verificado el email,
   `PENDING_VERIFICATION` si no. No se vuelve a gestionar sola: eso lo decide el profesor.
9. **La lista de usuarios filtra por rol, estado y email parcial.** Al profesor se le negó la
   búsqueda parcial para que no pudiera enumerar cuentas; el admin ya puede verlas todas, así
   que no hay nada que proteger y sí mucho que buscar.
10. **Pantallas** en `/admin`: Configuración (los dos números) y Usuarios (lista con filtros y
    el botón de activar o desactivar, con confirmación). El admin entra ahí al iniciar sesión.

Fuera: que el admin cancele clases (`01` §17 lo prevé; hoy sólo cancela reservas) y la
auditoría formal (`01` §16). Se anotan para la Fase 13.

## Contrato

- `PATCH /admin/configuration` → `PlatformConfiguration`. `400 VALIDATION_ERROR` si un valor no
  es positivo. Se retira `/admin/configuration/student-limit`.
- `GET /admin/users?role&status&query&page&size` → `AdminUserSummaryPage`.
- `PATCH /admin/users/{id}/status` `{status: ACTIVE|DISABLED}` → `AdminUserSummary`. `404` si
  no existe, `403 ADMIN_TARGET_NOT_ALLOWED` si no es un alumno.
- Todo `/admin/**` responde `403 AUTH_FORBIDDEN` a quien no sea `ADMIN`.

## Criterios de aceptación

- Un profesor y un alumno reciben `403` en todo `/admin/**` (test por endpoint).
- El bootstrap crea el admin si no existe y no lo duplica al reiniciar.
- Cambiar el límite se refleja en la siguiente gestión de un alumno: con el límite alcanzado,
  `STUDENT_LIMIT_REACHED`.
- Desactivar a un alumno: no puede iniciar sesión, su refresh da `401`, y sus reservas futuras
  quedan `CANCELLED_BY_ADMIN` y liberan la plaza.
- Reactivarlo le devuelve el estado que tenía, y puede volver a iniciar sesión.
- Desactivar al profesor o a un admin responde `403 ADMIN_TARGET_NOT_ALLOWED`.
- ArchUnit sigue en verde con `administration` usando sólo puertos de entrada.
- Daniel recorre en el navegador: entra como admin, cambia el límite, desactiva a un alumno y
  comprueba que no puede entrar, y lo reactiva.
