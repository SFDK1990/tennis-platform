# Fase 5 — Análisis: seguridad y autenticación (módulo `identity`)

Documento de análisis previo a la implementación, conforme a `12-metodologia-trabajo.md`: **no contiene código** y la fase no avanza hasta que las decisiones abiertas de la sección 8 estén resueltas.

## 1. Alcance

Entra en esta fase:

- Registro público de alumnos.
- Verificación de email.
- Login y emisión de tokens.
- Rotación de refresh tokens y detección de reutilización.
- Logout revocable.
- Recuperación y restablecimiento de contraseña.
- Bootstrap de la cuenta del profesor.
- Infraestructura de autorización por rol y por pertenencia, que los módulos posteriores consumirán.
- Rate limiting de los endpoints anteriores.

No entra: perfiles de usuario (Fase 6), gestión de alumnos por el profesor (Fase 6), ni MFA (riesgo aceptado en `02-arquitectura.md`).

## 2. Endpoints

Ya fijados en `openapi.yaml`; esta fase los implementa, no los redefine.

| Método | Ruta | Autenticación |
|---|---|---|
| POST | `/api/v1/auth/register` | Pública |
| POST | `/api/v1/auth/login` | Pública |
| POST | `/api/v1/auth/refresh` | Cookie de refresh |
| POST | `/api/v1/auth/logout` | Cookie de refresh |
| POST | `/api/v1/auth/verify-email` | Pública (token de un solo uso) |
| POST | `/api/v1/auth/forgot-password` | Pública |
| POST | `/api/v1/auth/reset-password` | Pública (token de un solo uso) |
| GET | `/api/v1/me` | Bearer |

## 3. Modelo de datos

Ya definido en `10-diagrama-er.md`, changelog 1: `users`, `email_verifications`, `password_reset_tokens` y `refresh_tokens`. Esta fase lo convierte en changesets reales de Liquibase.

Dos invariantes se apoyan en el esquema, no en la aplicación:

- `ux_users_email` — un email, una cuenta.
- `ux_users_single_teacher` — índice único parcial que garantiza que solo exista una fila con `role = 'TEACHER'`, materializando en base de datos la decisión "MVP con un único profesor".

## 4. Estados y ciclo de vida

`users.status` admite tres valores:

- `PENDING_VERIFICATION` — alta recién creada, email sin verificar.
- `ACTIVE` — email verificado.
- `DISABLED` — desactivada por un administrador.

Una cuenta `DISABLED` no puede iniciar sesión, y sus refresh tokens deben quedar revocados en el momento de la desactivación; de lo contrario podría seguir operando hasta que caduquen.

## 5. Flujos

### Registro

El cliente envía email y contraseña. El email se normaliza a minúsculas antes de cualquier comprobación. Se crea el usuario con rol `STUDENT` y estado `PENDING_VERIFICATION`, se genera un token de verificación de un solo uso y se envía por correo.

**La respuesta es idéntica exista o no la cuenta**, para no permitir enumeración de usuarios (`02-arquitectura.md`). Si el email ya está registrado no se crea nada y no se revela nada; opcionalmente se envía un aviso a la dirección real informando del intento.

El rol nunca se acepta desde el cliente: registrarse siempre produce un `STUDENT`.

### Verificación de email

El token llega por correo, se almacena solo su hash y caduca. Al usarse marca `used_at` y pasa el usuario a `ACTIVE` con `email_verified_at`. Un token usado o caducado se rechaza sin distinguir entre ambos casos.

### Login

Se comprueba la contraseña contra el hash almacenado. **La comparación se ejecuta siempre**, incluso cuando el email no existe, contra un hash ficticio, para que el tiempo de respuesta no delate qué cuentas existen.

Éxito: se emite un access token en el cuerpo y un refresh token en cookie `HttpOnly`, iniciando una nueva familia de rotación.

### Rotación del refresh token

Cada uso del refresh token lo invalida y emite uno nuevo dentro de la misma familia (`family_id`), encadenado mediante `replaced_by_token_id`.

Si llega un refresh token que ya fue reemplazado, se interpreta como robo: **se revoca la familia completa**, no solo ese token. El atacante y la víctima quedan ambos fuera, lo cual es el comportamiento correcto —es preferible obligar a la víctima a iniciar sesión de nuevo que dejar viva la sesión del atacante—.

### Recuperación de contraseña

`forgot-password` responde siempre igual, exista o no la cuenta. El token es de un solo uso y con caducidad corta. Al restablecer la contraseña se revocan **todas** las sesiones activas del usuario: si la recuperación se debe a un compromiso, dejar vivas las sesiones anteriores anularía el propósito.

## 6. Autorización

Dos dimensiones, según `02-arquitectura.md`: rol **y** propiedad o relación con el recurso. Esta fase construye la infraestructura; las reglas concretas se aplican en cada módulo posterior.

La regla que debe quedar imposible de saltar: **la identidad del sujeto se toma siempre del token**, nunca de un campo del DTO o de un parámetro de ruta. Un identificador de usuario que llegue en el cuerpo de una petición se ignora.

## 7. Estructura del módulo

Respeta la arquitectura hexagonal ya definida, sin dependencias hacia otros módulos (`identity` no depende de nadie):

```
identity/
  domain/                    Usuario, rol, estado, tokens, reglas e invariantes
  application/port/in/       Casos de uso: registrar, verificar, login, refrescar,
                             cerrar sesión, solicitar y restablecer contraseña
  application/port/out/      Puertos: repositorios, envío de email, reloj,
                             generación de tokens
  application/service/       Implementación de los casos de uso y transacciones
  adapters/in/web/           Controladores REST y DTOs
  adapters/out/persistence/  Entidades JPA y repositorios
  adapters/out/email/        Adaptador SMTP
  configuration/             Cableado de Spring, filtro JWT, codificador de contraseñas
```

El dominio no conoce Spring, JPA ni HTTP. El reloj se inyecta como puerto para poder probar caducidades sin esperar en tiempo real.

## 8. Decisiones abiertas

Deben resolverse antes de implementar. Cada una lleva una recomendación razonada.

### 8.1 Duración de los tokens

**Recomendación:** access token 15 minutos, refresh token 14 días.

Quince minutos acota la ventana de uso de un access token robado sin castigar la experiencia, porque la rotación es transparente. Catorce días evita que el usuario tenga que autenticarse constantemente en una PWA de uso semanal.

### 8.2 Algoritmo de firma del JWT

**Recomendación:** HS256 con secreto en variable de entorno.

RS256 aporta valor cuando terceros deben verificar el token sin poder emitirlo. Aquí el mismo monolito firma y verifica, así que la clave asimétrica añade gestión de claves sin beneficio. Migrar a RS256 más adelante es viable si aparecen consumidores externos.

El secreto no tiene valor por defecto en el perfil de producción: si falta, la aplicación no arranca.

### 8.3 Algoritmo de hash de contraseñas

`02-arquitectura.md` admite Argon2id o BCrypt.

**Recomendación:** BCrypt con coste 12, gestionado a través del codificador delegante de Spring Security, que almacena el algoritmo como prefijo del hash. Así se puede migrar a Argon2id más adelante **sin invalidar las contraseñas existentes**: los hashes antiguos se siguen verificando y se re-cifran al siguiente login.

Argon2id es preferible en abstracto, pero exige una dependencia criptográfica adicional y ajustar memoria y paralelismo; con el prefijo del delegante, esa decisión deja de ser irreversible.

### 8.4 Bootstrap del profesor — corrige una decisión previa

`00-indice-arquitectura.md` dice "bootstrap por seed/migración". **Señalo esto como decisión previa a corregir**, conforme a la regla de no mantener malas decisiones por compatibilidad.

Un changeset de Liquibase es un archivo versionado en Git, y este repositorio ya está publicado en GitHub. Meter ahí la contraseña inicial del profesor —en claro o como hash— la deja en el historial para siempre, y un hash de BCrypt en un repositorio es material atacable offline.

**Recomendación:** el changeset de Liquibase crea el esquema, pero **no** la cuenta. La cuenta la crea un componente de arranque idempotente que lee las credenciales de variables de entorno: si ya existe un usuario con rol `TEACHER`, no hace nada. Ninguna credencial toca el repositorio.

### 8.5 ¿La verificación de email bloquea el login?

`02-arquitectura.md` dice "email verificado antes de reservar", no antes de entrar.

**Recomendación:** permitir el login en estado `PENDING_VERIFICATION` y bloquear únicamente la reserva. Así el usuario puede entrar, ver la aplicación y reenviarse el correo de verificación. Si la verificación bloqueara el login, un correo perdido dejaría la cuenta inaccesible sin margen de maniobra.

Consecuencia para la Fase 9: reservar debe comprobar el estado `ACTIVE`, no solo el rol.

### 8.6 Entrega de correo en desarrollo

No hay servidor SMTP disponible en local, y los flujos de verificación y recuperación dependen del correo.

**Recomendación:** añadir un contenedor de buzón de pruebas al `compose.yaml` que capture todo el correo saliente y lo muestre en una interfaz web. Evita tener que registrar un proveedor real para desarrollar, y no altera el código: solo la configuración SMTP del perfil `dev`.

### 8.7 Rate limiting

**Recomendación:** limitación en memoria por IP y por cuenta para los endpoints de autenticación, sin introducir Redis.

Una sola instancia no necesita estado compartido, y añadir Redis ahora contradice el principio de no incorporar complejidad innecesaria. El día que haya varias instancias, este componente es el que habrá que sustituir, y conviene dejarlo aislado tras un puerto para que ese cambio sea local.

### 8.8 Política de la cookie y CSRF

**Recomendación:** `SameSite=Strict` para la cookie de refresh.

Los enlaces de verificación y recuperación llegan por correo, pero apuntan a páginas del frontend que llaman a la API por su cuenta; no dependen de que la cookie de refresh viaje en la navegación inicial, así que `Strict` no rompe esos flujos y es más restrictivo que `Lax`.

Con `Strict` el riesgo de CSRF queda muy acotado, pero `02-arquitectura.md` exige protección explícita en los endpoints que aceptan la cookie: se mantiene, con un token de doble envío en `/auth/refresh` y `/auth/logout`.

## 9. Estrategia de testing

Conforme a la regla aprobada, el testing es criterio de salida de esta fase, no de una fase posterior.

- **Dominio, sin Spring:** reglas de caducidad, estados de token, transiciones de estado del usuario, invariantes de rol.
- **Aplicación, con puertos simulados:** cada caso de uso, incluyendo reloj controlado para probar caducidades sin esperas reales.
- **Persistencia e integración, con PostgreSQL real:** que los índices únicos y las restricciones hagan su trabajo, en particular el de profesor único.
- **API:** códigos de estado, formato `application/problem+json` y atributos de la cookie.
- **Seguridad, los más importantes:**
  - Registro con email existente y con email nuevo producen **respuestas indistinguibles**.
  - `forgot-password` responde igual exista o no la cuenta.
  - Reutilizar un refresh token ya rotado **revoca la familia completa**.
  - Un access token caducado, manipulado o firmado con otra clave se rechaza.
  - Restablecer la contraseña invalida todas las sesiones previas.
  - Una cuenta `DISABLED` no puede iniciar sesión ni refrescar.
  - Ningún endpoint devuelve el hash de contraseña ni el refresh token en el cuerpo.
  - Ninguna traza de log contiene contraseñas ni tokens.

## 10. Criterios de aceptación

La fase se considera terminada cuando, con la salida real de los comandos pegada como evidencia:

1. Un alumno puede registrarse, verificar su email, iniciar sesión, refrescar y cerrar sesión, con la secuencia completa demostrada por un test de integración.
2. Existe un test que demuestra que reutilizar un refresh token ya rotado revoca la familia entera.
3. Existe un test que demuestra que registro y recuperación no permiten distinguir si una cuenta existe.
4. Existe un test que demuestra que el esquema impide un segundo usuario con rol `TEACHER`.
5. El bootstrap del profesor es idempotente: ejecutarlo dos veces no crea una segunda cuenta ni falla.
6. Ninguna credencial ni secreto aparece en el repositorio.
7. La suite completa pasa con **cero tests saltados**.
8. La aplicación arranca con `docker compose up` y los flujos funcionan de extremo a extremo.

## 11. Riesgos

- **Correo en producción:** sin proveedor SMTP real decidido, la verificación y la recuperación no son utilizables fuera de desarrollo. No bloquea esta fase, pero sí el lanzamiento.
- **Rotación de la clave de firma:** no hay mecanismo previsto para cambiar el secreto sin invalidar todas las sesiones. Aceptable en el MVP; conviene registrarlo como deuda.
- **Limpieza de tokens caducados:** las tres tablas de tokens crecen indefinidamente. Hace falta un borrado periódico, que puede quedar fuera de esta fase pero debe anotarse.
- **Bloqueo por intentos fallidos:** el rate limiting por IP no impide un ataque distribuido contra una cuenta concreta. El bloqueo temporal de cuenta queda fuera del MVP, en línea con los riesgos ya aceptados.
