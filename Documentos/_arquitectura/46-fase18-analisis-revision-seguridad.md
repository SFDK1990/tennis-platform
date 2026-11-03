# Fase 18 — Análisis: revisión final de seguridad

Criterio de salida (`12-metodologia-trabajo.md`): informe con severidades; nada se aplica sin la
aprobación de Daniel. Esta revisión parte de la Fase 15 (`26-fase15-analisis-seguridad.md`) y
mira sobre todo lo que ha entrado después: pagos, fotos, mensajes, grupos, PWA y el despliegue en
la Pi. Se ha hecho como lo haría un atacante, con la lista de Daniel del 03/11/2026 como guion.

## Cómo se ha revisado

- **Pruebas activas sólo en local**, con la misma cadena que producción: Caddy con el
  `Caddyfile` real → Next (`next build` de producción) → backend con el perfil `prod` →
  PostgreSQL. La Pi no se ha tocado.
- Lectura del código de `identity`, `SecurityConfig`, `pass` (Stripe), la foto del alumno, el
  correo, el `.ics`, el proxy de Next y la configuración de despliegue.
- **Secretos:** los 316 commits de todas las ramas (`git log --all -p`), con patrones de claves de
  Stripe, AWS, GitHub, Slack, Google, claves privadas, JWT y contraseñas escritas a mano, más los
  ficheros añadidos y borrados en la historia. `gitleaks` no se pudo descargar en este entorno, así
  que la búsqueda es por patrones y no por su catálogo completo.
- **Dependencias:** `npm audit`, `depcheck`, la lista del `pom.xml` y el último `trivy` del CI en
  `main` (verde, 03/11/2026).

## Hallazgos

Por severidad. Cada uno con lo que se hizo para comprobarlo.

### 1. Alta — el límite de intentos de login se salta codificando una letra de la ruta

`AuthRateLimitFilter.shouldNotFilter` compara `request.getRequestURI()`, que es la ruta **tal como
llega, sin decodificar**, con `/api/v1/auth/`. Tomcat (para decidir si el filtro aplica) y Spring
MVC (para elegir el controlador) usan la ruta **decodificada**. Así que `/api/v1/%61uth/login`
(`%61` es la `a`) llega al login de siempre, pero el filtro cree que no es suya y no cuenta.

Comprobado a través de Caddy → Next → backend, desde la misma IP:

```
/api/v1/auth/login    x21  → 401 ×20, 429           (el límite funciona)
/api/v1/%61uth/login  x25  → 401 ×25                (ningún 429)
/api/v1/%61uth/login  con la contraseña buena → 200 y accessToken
/api/v1/me/p%61ssword x3 tras agotar el cubo → 422 ×3 (tampoco cuenta)
```

- **Qué permite:** probar contraseñas sin límite contra Marcos, el admin o cualquier alumno, y
  contra la contraseña actual con un token robado (`/me/password`, `/me/deletion`). Lo frenan sólo
  bcrypt (coste 12) y el mínimo de 10 caracteres.
- **Arreglo propuesto:** que el filtro mire la ruta ya decodificada y normalizada
  (`getServletPath()` + `getPathInfo()`), la misma que usa Tomcat para mapearlo. Test: con el cubo
  agotado, `/api/v1/%61uth/login` y `/api/v1/me/p%61ssword` dan 429. Se comprueba rompiéndolo:
  volviendo a `getRequestURI()`, el test falla.

### 2. Media — no hay límite de tamaño del cuerpo de las peticiones

Ni Caddy ni el backend ponen tope. Next corta a 10 MB, pero sigue reenviando y la conexión queda
colgada 30 segundos hasta dar un 500. El backend, llamado directamente, aceptó y procesó un JSON de
60 MB en `POST /auth/register` (202).

- **Qué permite:** a alguien sin cuenta, gastar memoria y conexiones de una Raspberry Pi con
  1 GB para el backend mandando cuerpos de 10 MB a rutas públicas. Con el hallazgo 1, sin que el
  límite por IP lo frene.
- **Arreglo propuesto:** `request_body { max_size 2MB }` en el `Caddyfile` (la foto, lo más
  grande que se sube, tiene 1 MB como máximo). Así el exceso se corta en la entrada con un 413 y
  no llega a Next. Prueba: un cuerpo de 3 MB a través de Caddy da 413.

### 3. Baja — la aplicación entra en PostgreSQL como superusuario

`compose.prod.yaml` crea la base con `POSTGRES_USER`, que es superusuario, y el backend se conecta
con ese mismo usuario. No hay SQL concatenado (ver abajo), así que hoy no es explotable, pero si un
día lo fuera, el atacante tendría la base entera y podría leer ficheros del contenedor
(`COPY ... FROM`).

- **Arreglo propuesto:** un rol propio para la aplicación, dueño de su base pero sin
  `SUPERUSER`, `CREATEROLE` ni `CREATEDB`. Liquibase sigue pudiendo migrar porque es el dueño de
  las tablas. En la Pi exige un paso manual que se documentaría en `33-operacion.md`. Es el
  único arreglo de la lista que toca producción.

### 4. Baja — el correo puede salir sin cifrar

`application-prod.yml` pone `mail.smtp.starttls.enable: true` pero no
`mail.smtp.starttls.required`. Si alguien en medio quita el `STARTTLS` del servidor, JavaMail
sigue en claro: la contraseña de Brevo y los enlaces de restablecimiento viajarían sin cifrar.

- **Arreglo propuesto:** `mail.smtp.starttls.required: true`. Una línea.

### 5. Baja — la cookie `XSRF-TOKEN` sale sin `Secure` ni `SameSite`

Visto en la respuesta del login: `Set-Cookie: XSRF-TOKEN=…; Path=/`. JavaScript tiene que poder
leerla (es el doble envío del CSRF), pero no hay razón para que viaje por `http` ni a otros sitios.
La de sesión (`refresh_token`) sí lleva `Secure; HttpOnly; SameSite=Strict`.

- **Arreglo propuesto:** `cookieCustomizer` en `SecurityConfig` con `Secure` (el mismo
  interruptor `cookie-secure` que la de sesión) y `SameSite=Strict`.

### 6. Baja — las rutas de admin sólo se protegen dentro de cada controlador

`/api/v1/admin/**` cae en `anyRequest().authenticated()`; el "sólo admin" lo dice cada método
(`caller.isAdmin()`). Funciona y la matriz de acceso de la Fase 15 lo prueba ruta a ruta, pero un
método nuevo que olvide la comprobación sólo lo pararía ese test.

- **Arreglo propuesto:** `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` en
  `SecurityConfig`, como segunda barrera. Las comprobaciones de los controladores se quedan.

### 7. Informativo — credenciales de desarrollo en el historial de Git

Un informe ya borrado (`Documentos/_informes/informe-arranque-local-2026-09-19.md`) sigue en el
historial con dos cuentas del entorno local:

- `profesor@tennisplatform.local` con su contraseña de desarrollo;
- `alumno1@tennisplatform.local` con la suya.

Son cuentas `.local` de la base de desarrollo y el repositorio es privado. Sólo importa si alguna
de esas dos contraseñas se ha reutilizado en otro sitio: el `.env` de la Pi, Brevo, Stripe,
Tailscale o un correo personal. **Lo tiene que confirmar Daniel.** Si se reutilizó, se cambia allí;
reescribir la historia no hace falta.

No se ha encontrado ninguna otra cosa: ni claves de Stripe (`sk_live`, `sk_test`, `whsec`), ni
tokens de GitHub o de nube, ni claves privadas, ni `.env` reales. `backup.pub` es la clave
**pública** de los backups, que es justo lo que tiene que estar.

### 8. Informativo — sin alerta en tiempo real

Hay registro de cada petición con su código de error, métricas para el admin y el informe semanal
de accesos (`access-report.sh`). No hay nada que avise en el momento de una ráfaga de `401` o
`429`. Con un solo profesor y una Pi es razonable; si Daniel lo quiere, la versión más barata es
que el informe semanal cuente también `AUTH_INVALID_CREDENTIALS` y `AUTH_RATE_LIMITED` por día.

### 9. Informativo — dependencias

- `npm audit --omit=dev`: 0. Las 18 moderadas que da `npm audit` son todas de `minimatch` a
  través de ESLint, que no llega a la imagen.
- `depcheck` sólo señala Tailwind, que se usa por PostCSS: falso positivo. No sobra ningún paquete
  en el frontend ni en el `pom.xml`.
- `trivy` (HIGH/CRITICAL con arreglo) sigue en el CI de las dos imágenes, en verde en `main`.

## Lo que está bien

Revisado contra la lista de Daniel y sin cambios que proponer:

- **Autenticación y sesión:** access token de 15 minutos en memoria; refresh rotatorio con
  detección de reutilización, `HttpOnly`/`Secure`/`SameSite=Strict`, guardado como hash. Un JWT
  con `alg: none` o con el rol cambiado da 401; el profesor contra `/admin` da 403.
- **Contraseñas:** bcrypt con coste 12, de 10 a 72 bytes.
- **Enlaces de recuperación:** caducan en 1 hora, son de un solo uso, se guardan como hash, y
  usar uno invalida los demás.
- **Claves fuera del navegador:** el frontend no lleva ninguna clave; Stripe se usa sólo desde el
  backend.
- **Pagos:** el precio sale del tipo de bono en el servidor, el bono nace sólo con el aviso firmado
  de Stripe (HMAC en tiempo constante, 5 minutos de tolerancia) y un importe distinto no crea bono.
- **Validación en el servidor:** Bean Validation en los DTO; los campos que no tocan (`role`,
  `status`, `id`) se ignoran en `PATCH /me`, comprobado.
- **SQL:** sólo JPA, `Specification` y una consulta nativa con parámetros; el `LIKE` escapa los
  comodines.
- **XSS:** React escapa; el único `dangerouslySetInnerHTML` es el JSON-LD estático con `<`
  escapado; CSP con nonce y `strict-dynamic`; los correos son texto plano; el `.ics` escapa saltos
  de línea.
- **Subidas:** sólo la foto, 1 MB, tipo leído de los primeros bytes (JPEG, PNG, WebP) y servida con
  `no-store` y `nosniff`.
- **Cabeceras:** CSP, `frame-ancestors 'none'`, `X-Frame-Options`, `nosniff`, `Referrer-Policy`,
  `Permissions-Policy`, HSTS; sin `X-Powered-By`.
- **CORS:** no se abre a nadie; un origen ajeno no recibe `Access-Control-Allow-Origin`. `TRACE`
  da 405.
- **HTTPS:** Funnel sólo sirve HTTPS; Caddy y el backend no tienen puerto público.
- **Ficheros expuestos:** `/.env`, `/.git/config`, `/openapi.yaml`, `/v3/api-docs`,
  `/actuator/*` (también con `..` y `%2e%2e`), `/package.json` y `/server.js` dan 404 a través de
  Caddy.
- **Modo debug:** perfil `prod` sin detalles en `health`, log en `WARN`, errores 500 sin traza y
  build de Next de producción.
- **Base de datos:** sin puertos publicados en producción, contraseña obligatoria, backups
  cifrados con clave pública.

## Lo que no se ha podido comprobar desde aquí

- Que Tailscale Funnel sobrescriba `X-Forwarded-For` con la IP real del cliente: está en la
  documentación de la Fase 17 y no se ha probado contra la Pi.
- Los permisos del `.env` en la Pi (`chmod 600`) y quién está en el grupo `docker` (puede leer los
  secretos con `docker inspect`).

## Decisiones propuestas

Daniel aprobó todos el 03/11/2026. Cada uno, con su test o prueba y comprobado rompiéndolo:

1. Arreglar el límite de intentos (hallazgo 1). **Recomendado ya.**
2. Límite de 2 MB en Caddy (hallazgo 2). **Recomendado ya.**
3. `starttls.required` (hallazgo 4), cookie `XSRF-TOKEN` con `Secure` y `SameSite` (hallazgo 5) y
   la barrera de `/admin` en `SecurityConfig` (hallazgo 6). Pequeños y sin riesgo.
4. Rol sin superusuario en PostgreSQL (hallazgo 3): necesita un paso manual en la Pi; se puede
   dejar para el siguiente despliegue.
5. Contar los fallos de login en el informe semanal (hallazgo 8): opcional.

## Decisiones tomadas al implementar

1. **El filtro mira `getServletPath()` + `getPathInfo()`**, la ruta decodificada y normalizada con
   la que Tomcat ya lo mapeaba, en vez de decodificar por su cuenta: así no puede haber dos
   opiniones sobre qué ruta es. `AuthRateLimitTest` agota el cubo y prueba `/api/v1/%61uth/…` y
   `…/forgot-p%61ssword`: los dos dan 429. Con `getRequestURI()` el test falla. Repetido a mano a
   través de Caddy y Next: `%61uth` y `l%6Fgin` dan 429; `%41uth` (`Auth`) no llega al login.
2. **El tope de 2 MB va en Caddy**, la entrada, y no en el backend: así un cuerpo grande no llega
   a ocupar Next. Comprobado: 3 MB (con `Content-Length` y en *chunked*) da 413 al instante; 1,5 MB
   pasa.
3. **El rol propio es `tennis_app`, y `DB_USER` sigue siendo el superusuario** para backups,
   restauraciones y `psql` a mano. El superusuario inicial de la imagen no puede dejar de serlo,
   así que no bastaba con quitárselo.
   - Un solo script (`deploy/postgres-init/app-role.psql`), idempotente, crea el rol y le pasa
     la base y sus tablas. Lo ejecutan la imagen al crear el volumen, `deploy.sh` en cada
     despliegue (antes de arrancar el backend) y `restore.sh` tras restaurar, porque el dump no
     lleva dueños.
   - Probado en las dos situaciones: sobre una base con datos creada por el superusuario (31
     tablas pasan a `tennis_app`; el login sigue funcionando) y sobre un volumen nuevo (Liquibase
     migra entero como `tennis_app`, con `pgcrypto` y `btree_gist`, que son extensiones de
     confianza). Como `tennis_app`, `COPY … FROM '/etc/passwd'` se rechaza; como superusuario lee
     el fichero.
   - El CI arranca la composición de producción y falla si el rol del backend es superusuario o
     no es dueño de las tablas.
   - En la Pi hace falta añadir `DB_APP_PASSWORD` al `.env` antes del primer despliegue
     (`33-operacion.md`). Sin él, `compose` se niega a arrancar y dice qué falta.
4. **`starttls.required`**: con Mailpit, que no cifra, el backend ahora se niega a enviar
   («STARTTLS is required but host does not support STARTTLS»). Brevo sí lo ofrece en el 587.
5. **Cookie `XSRF-TOKEN`** con el mismo interruptor `cookie-secure` que la de sesión y
   `SameSite=Strict`. Un test de `AuthFlowIntegrationTest` lo comprueba y falla sin el cambio.
6. **Barrera de `/admin`**: un test pide `/api/v1/admin/anything`, que no tiene controlador, con
   un alumno. Da 403 `AUTH_FORBIDDEN`, y fuera de `/admin` la misma petición da 404. Sin la línea
   en `SecurityConfig`, el test falla.
7. **Informe semanal**: una sección nueva cuenta por día las contraseñas fallidas (login y
   contraseña actual) y los 429, y marca «revisar» a partir de 50 en un día. La sección de
   intentos ya existía. Su comentario citaba un código que no existe (`AUTH_BAD_CREDENTIALS`);
   ahora cita el real.
8. **Credenciales del historial (hallazgo 7)**: queda pendiente de que Daniel confirme que no se
   reutilizaron. No se reescribe la historia.

9. **Lo que enseñó el despliegue de `v0.29.0` en la Pi** (03/11/2026):
   - `deploy.sh` no recreaba Caddy, y Caddy seguía leyendo el `Caddyfile` viejo: el tope de 2 MB
     no se aplicó hasta recrearlo a mano. Ahora `deploy.sh` lo recrea siempre (un segundo).
   - Tampoco actualizaba nunca las imágenes que no construimos (`postgres`, `caddy`) ni las bases
     de las que sí: sus parches de seguridad no llegaban a la Pi. Ahora hace `pull` y
     `build --pull` antes de parar nada.
   - El Caddy de la Pi (2.11.4) cortaba los cuerpos grandes con un 502, no con un 413. Una
     petición que declara más de 2 MB en `Content-Length` se rechaza ahora con 413 antes de leerla,
     sea cual sea la versión; `request_body` queda para las que no lo declaran.
