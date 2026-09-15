# Tennis Platform — Documento del Senior Frontend Engineer

## Stack

- Next.js.
- React.
- TypeScript estricto.
- Tailwind CSS.
- PWA.
- Cliente REST tipado.
- Tests de componentes.
- Playwright para E2E.

## Organización frontend

La aplicación se organizará por funcionalidad:

- `authentication`.
- `profile`.
- `students`.
- `availability`.
- `calendar`.
- `lessons`.
- `bookings`.
- `administration`.

No se creará una carpeta global con toda la lógica de negocio mezclada.

## Rutas principales

### Públicas

- Registro.
- Verificación de email.
- Login.
- Recuperación de contraseña.
- Restablecimiento de contraseña.

### Profesor

- Dashboard.
- Perfil.
- Alumnos.
- Disponibilidad.
- Calendario.
- Crear clase.
- Detalle de clase.
- Participantes.

### Alumno

- Dashboard.
- Perfil.
- Clases disponibles.
- Mis reservas.
- Próximas clases.
- Detalle de clase.

### Administración

- Configuración global.
- Límite de alumnos.
- Usuarios.
- Estado de usuarios.

## Consumo REST

El refresh token viaja en una cookie `HttpOnly`/`Secure`/`SameSite`; el frontend nunca lo lee ni lo almacena explícitamente, solo asegura que las peticiones se hagan con `credentials: 'include'`.

El frontend tendrá un cliente HTTP centralizado responsable de:

- Añadir credenciales.
- Gestionar refresh de sesión.
- Normalizar errores.
- Propagar correlation IDs si aplica.
- Convertir respuestas a tipos TypeScript.

Los componentes no realizarán llamadas HTTP directamente de forma dispersa.

## Estado

Se separará:

- Estado de sesión.
- Datos remotos.
- Estado local de formularios.
- Estado visual de calendarios.

La fuente de verdad para clases, reservas y disponibilidad será el backend.

## Calendario

El calendario deberá:

- Mostrar hora local del usuario.
- Indicar la zona horaria.
- Diferenciar disponibilidad, clases y reservas.
- Mostrar clases grupales y plazas disponibles.
- Mostrar estados cancelados y completados.
- Evitar crear reservas desde información obsoleta.

Antes de confirmar una reserva, el frontend debe asumir que el estado puede haber cambiado y manejar correctamente respuestas `409`.

## Formularios

Validación inmediata en frontend para UX, pero validación definitiva en backend.

Formularios principales:

- Registro.
- Perfil.
- Disponibilidad semanal.
- Excepciones.
- Crear clase.
- Reservar.
- Cancelar.
- Configuración administrativa.

## PWA

La PWA incluirá:

- Manifest.
- Service worker.
- Cache del shell.
- Pantalla offline.
- Última información consultada cuando sea seguro.

No se permitirán reservas ni cancelaciones offline porque requieren consistencia transaccional.

## Accesibilidad

- Navegación por teclado.
- Labels en formularios.
- Contraste suficiente.
- Mensajes de error asociados a campos.
- Estados de carga accesibles.
- Confirmaciones de acciones destructivas.

## Seguridad frontend

- No usar `localStorage` para refresh tokens.
- No mostrar DNI o dirección fuera de las vistas autorizadas.
- No asumir que ocultar un botón equivale a autorización.
- Limpiar datos sensibles al cerrar sesión.
- Mostrar claramente errores de sesión expirada.

## Testing frontend

- Componentes de formularios.
- Calendario.
- Estados de carga y error.
- Permisos por rol.
- Flujo de reserva.
- Cancelación dentro y fuera de 24 horas.
- Cambios de zona horaria.
- E2E de registro, login, gestión de alumno y reserva.
