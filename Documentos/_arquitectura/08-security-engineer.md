# Tennis Platform — Documento del Security Engineer

## Superficie de ataque

- Registro público.
- Login.
- Recuperación de contraseña.
- Tokens de sesión.
- Datos personales.
- Gestión de alumnos.
- Reservas concurrentes.
- Consola administrativa.
- PWA y almacenamiento del navegador.

## Identidad y autenticación

- Registro público inicial como `STUDENT`.
- Profesor creado mediante bootstrap (seed/migración con credenciales iniciales), no mediante registro público ni consola de administración.
- Email verificado antes de reservar.
- Contraseñas con Argon2id o BCrypt.
- Access tokens de corta duración.
- Refresh tokens rotatorios.
- Refresh tokens almacenados mediante hash.
- Tokens de recuperación de un solo uso y con caducidad.
- Cierre de sesión revocable.

No se almacenarán tokens sensibles en `localStorage`.

## Autorización

La autorización tendrá dos dimensiones:

1. Rol.
2. Propiedad o relación con el recurso.

Ejemplos:

- Un alumno solo puede ver sus reservas.
- Un alumno solo puede reservar si está gestionado.
- El profesor solo puede gestionar sus alumnos.
- Solo `ADMIN` puede cambiar el límite global.
- Solo el profesor puede crear o cancelar clases.

Ocultar controles en frontend no se considera autorización.

## Protección de datos

El sistema almacena:

- DNI.
- Dirección.
- Teléfono.
- Email.

Medidas:

- Acceso mínimo necesario.
- No devolver datos completos en listados innecesarios.
- No escribirlos en logs.
- Cifrado en tránsito.
- Cifrado de almacenamiento según entorno.
- Exportación controlada.
- Eliminación o anonimización conforme a la política aprobada.

## Riesgos específicos

### Enumeración de usuarios

Las respuestas de registro, recuperación y búsqueda no deben revelar innecesariamente si existe una cuenta.

### Reservas concurrentes

La seguridad de la capacidad no puede depender del frontend. Debe garantizarse mediante transacciones y constraints.

### Acceso horizontal

Se debe comprobar que cambiar un identificador en una URL no permite ver datos de otro usuario.

### PWA

No se deben cachear indiscriminadamente respuestas con datos personales. La cache offline deberá limitarse al shell y a información segura.

### CSRF y cookies

El refresh token se transporta en una cookie `HttpOnly`, `Secure` y `SameSite=Strict` (o `Lax` si el flujo de verificación de email por enlace lo requiere), nunca en el cuerpo de la respuesta ni en `localStorage`. Se debe restringir el origen mediante CORS y aplicar protección CSRF en los endpoints que aceptan la cookie (`/auth/refresh`, `/auth/logout`).

## Validación

- Validar todos los campos en backend.
- Normalizar email.
- Limitar tamaños de entrada.
- Rechazar formatos inválidos de DNI, teléfono y fechas.
- No confiar en roles enviados por el cliente.
- No aceptar cambios de propiedad mediante DTOs.

## Rate limiting

Aplicar límites para:

- Login.
- Registro.
- Verificación de email.
- Recuperación de contraseña.
- Refresh de sesión.
- Endpoints administrativos.

## Logging seguro

Nunca registrar:

- Contraseñas.
- Tokens.
- DNI completo.
- Dirección completa.
- Datos personales no necesarios.

Los errores mostrados al cliente no incluirán stack traces ni detalles internos de base de datos.

## Gestión administrativa

El rol `ADMIN` debe:

- Requerir una creación segura.
- No poder registrarse públicamente.
- Tener sesiones protegidas.
- Requerir autorización explícita en cada endpoint.
- Quedar preparado para MFA futuro.

## Riesgos aceptados temporalmente

- Sin backups automatizados en el MVP.
- Sin auditoría formal.
- Sin MFA inicial.
- Sin proveedor externo de gestión de secretos.

Estos riesgos deben revisarse antes de producción con usuarios reales.

## Revisión de seguridad previa al lanzamiento

- Revisión de permisos por endpoint.
- Pruebas de acceso horizontal.
- Pruebas de tokens expirados y revocados.
- Pruebas de recuperación de contraseña.
- Revisión de logs.
- Revisión de datos expuestos en PWA.
- Revisión de CORS y headers.
- Escaneo de dependencias.
- Revisión de secretos en CI/CD.
