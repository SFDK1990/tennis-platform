# Fase 15 — Análisis: auditoría de seguridad

Criterio de salida (`12-metodologia-trabajo.md`): OWASP Top 10, acceso horizontal, secretos y
dependencias revisados. Las reglas de partida están en `02-arquitectura.md` §13.

## Cómo se ha auditado

- Revisión del código de `identity`, `SecurityConfig`, los handlers de error y el proxy del
  frontend.
- Peticiones reales contra el stack local.
- `gitleaks` sobre todo el historial.
- `trivy` sobre la imagen del backend.
- `npm audit` sobre el frontend.

## Lo que está bien y se queda como está

- **Sesión.** El access token dura 15 minutos y sólo vive en memoria. El refresh es rotatorio,
  va en una cookie `HttpOnly` + `Secure` + `SameSite=Strict` con path `/api/v1/auth`, se guarda
  como hash y detecta la reutilización.
- **CSRF** en las dos rutas que autentica la cookie.
- **JWT.** El algoritmo lo fija la clave (no acepta `none`) y se exige el emisor. Un secreto de
  menos de 32 caracteres impide arrancar, y producción no tiene valor por defecto.
- **Sin enumeración.** Registro, recuperación y login responden igual exista o no la cuenta. El
  login incluso gasta el tiempo de bcrypt.
- **Tokens de un solo uso** con caducidad y guardados como hash.
- **Contraseñas.** Bcrypt, un mínimo de 10 caracteres y un máximo de 72 bytes.
- **Acceso.** La identidad sale siempre del token, nunca del cuerpo ni de la ruta, y lo ajeno
  responde 404.
- **Inyección.** No hay SQL nativo ni concatenado, sólo JPA y `Specification`.
- **Cabeceras sospechosas.** El correlation id se filtra contra la inyección en logs y en
  cabeceras.
- **Tamaño de página.** Está acotado en todos los listados.
- **Contenedor.** El backend no corre como root.
- **CI.** El token de GitHub es de sólo lectura (`permissions: contents: read`).
- **Secretos.** `gitleaks` revisó los 73 commits y no encontró nada. `.env` no se versiona.
- **Frontend.** Sin `dangerouslySetInnerHTML` y sin redirecciones construidas con parámetros de
  la URL. `npm audit` da 0 vulnerabilidades.

## Hallazgos

Por severidad. Cada uno se cierra con un test o un check que falla si vuelve.

1. **Alta — dependencias con CVE conocidos** (A06). `trivy` sobre la imagen:
   - `tomcat-embed-core` 10.1.55: tres CRITICAL, corregidos en 10.1.58. Uno es un bypass de
     restricciones de seguridad.
   - `postgresql` 42.7.11: un HIGH, una degradación de SCRAM que permite un MITM. Corregido en
     42.7.12.

   Las dos llegan como transitivas del BOM de Spring Boot, que aún no las ha subido, y
   Dependabot no propone versiones de lo que gestiona el BOM. Es decir, hoy nada avisa.
2. **Alta — el rate limiting no funciona detrás del frontend** (A07). El navegador sólo habla
   con Next, que reenvía `/api` al backend. El backend cuenta por la IP del socket, y esa IP es
   la de Next para todos los usuarios, así que todos comparten un único cubo de 20 peticiones
   por minuto.
   - Cualquiera puede dejar sin login a toda la plataforma con 21 peticiones por minuto.
   - Next tampoco ayuda: rellena `X-Forwarded-For` sólo si no viene, así que la que manda el
     cliente pasa tal cual y es falsificable.
3. **Media — errores del cliente respondidos como 500** (A05). Lo comprobé con peticiones reales:
   una ruta que no existe, un método no soportado (`DELETE /me`) o un `Content-Type` equivocado
   responden `500 Unexpected error` y dejan una traza `ERROR` en el log. El catch-all de
   `GlobalExceptionHandler` se traga los 404, 405 y 415 de Spring.
   - Consecuencia: basta un escáner para llenar el log de falsos errores, y un error real se
     pierde entre ellos.
   - Además, el `500` sale sin `code`, en contra del contrato de errores.
4. **Media — el frontend no envía ninguna cabecera de seguridad** (A05). Las pantallas las sirve
   Next, y el `X-Frame-Options: DENY` del backend sólo protege el JSON. Faltan:
   - `frame-ancestors`: sin ella se puede hacer clickjacking sobre "Cancelar clase";
   - `Content-Security-Policy`: es la defensa que limitaría un XSS, y un XSS es lo único que
     puede leer el access token en memoria;
   - `Referrer-Policy`: los enlaces de verificación y de restablecimiento llevan el token en la
     URL.

   Además, `X-Powered-By: Next.js` anuncia el framework.
5. **Media — bombardeo de correo** (A04). `forgot-password` y el registro de una dirección que ya
   existe envían un correo en cada petición. Con el límite por IP, eso son 20 correos por minuto
   a la víctima, y más rotando IPs. El correo sale de nuestro dominio, así que la reputación del
   remitente también se resiente.
6. **Baja — los otros enlaces de restablecimiento siguen valiendo tras usar uno** (A07). Si se
   pidieron dos, cambiar la contraseña con el primero deja el segundo vivo durante una hora. La
   OWASP ASVS pide invalidarlos.
7. **Baja — el `health` detallado lo ve cualquier usuario con sesión** en desarrollo
   (`show-details: when-authorized` sin roles): versión de la base, disco, rutas. En producción
   ya es `never`.
8. **Baja — `compose.yaml` publica Postgres en todas las interfaces**, con la contraseña por
   defecto. En una red compartida, cualquiera del wifi entra a la base. Lo mismo pasa con Mailpit
   y el backend.
9. **Baja — las actions de CI van por etiqueta, no por SHA** (A08). Una etiqueta se puede mover;
   un SHA no.

**Acceso horizontal.** Las comprobaciones existen y están probadas, pero repartidas en tests de
cada módulo, 25 en total. Nada garantiza que un endpoint nuevo traiga la suya. Es el mismo
problema que la Fase 13 resolvió para el contrato.

## Decisiones propuestas

1. **Dependencias:**
   - Se fijan `tomcat.version` 10.1.60 y `postgresql.version` 42.7.13 en el `pom.xml`, con un
     comentario que diga cuándo quitarlas: cuando el BOM las alcance.
   - **`trivy` entra en CI**, sobre la imagen que ya construye el job del stack, y falla ante un
     HIGH o CRITICAL que tenga versión corregida. No necesita clave de NVD, que era la objeción
     contra OWASP Dependency-Check (`dependabot.yml`).
   - `npm audit --omit=dev --audit-level=high` entra en el job del frontend.
2. **La IP del cliente sale de una lista de proxies de confianza**
   (`tennis.identity.trusted-proxies`). Si el socket es uno de ellos, se usa `X-Forwarded-For`;
   si no, el socket. Por defecto, loopback y redes privadas, que es donde vive Next junto al
   backend.
   - Para que el valor sea fiable, el proxy de entrada tiene que sobrescribir la cabecera, no
     añadirle. Eso lo decide la Fase 17, con el proveedor, y queda anotado allí como requisito.
   - Si el vaciado del mapa de cubos lo provoca un atacante rotando IPs, se pierden los
     contadores de todos. Se sustituye por un expulsado de los más antiguos (Caffeine con
     tamaño máximo).
3. **Los errores de protocolo tienen su estado**: 404 para una ruta desconocida, 405, 406 y 415.
   Llevan `code` (`NOT_FOUND`, `METHOD_NOT_ALLOWED`, `NOT_ACCEPTABLE`, `UNSUPPORTED_MEDIA_TYPE`)
   y se registran en `DEBUG`, no en `ERROR`. El `500` pasa a llevar `code: INTERNAL_ERROR`, y un
   test demuestra que no incluye ni el mensaje ni la traza.
4. **Cabeceras del frontend**, en `next.config.ts`:
   - `Content-Security-Policy` con `frame-ancestors 'none'`, `object-src 'none'`,
     `base-uri 'self'`, `form-action 'self'` y `connect-src 'self'`.
   - `script-src` con **nonce** generado en `proxy.ts`, que es como Next 16 lo soporta. Obliga
     a renderizar las páginas en cada petición, y aquí no cuesta nada: todas son de usuario con
     sesión y se pintan en el cliente. Sin nonce haría falta `'unsafe-inline'`, y la CSP no
     pararía un XSS, que es para lo que se pone.
   - `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, `Permissions-Policy`
     sin cámara, micrófono ni geolocalización, y HSTS (el navegador lo ignora en `http`).
   - `poweredByHeader: false`.
5. **Un correo de cada tipo por dirección cada 5 minutos** (restablecimiento, intento de registro
   sobre una cuenta existente y reenvío de verificación). La respuesta no cambia (202), para no
   delatar nada. Va en memoria y acotado, igual que el rate limiting y por la misma razón: hay
   una sola instancia.
6. **Restablecer la contraseña invalida los demás enlaces de restablecimiento** del usuario.
7. **`health` sin detalles fuera de `ADMIN`**, en todos los perfiles.
8. **`compose.yaml` publica los puertos sólo en `127.0.0.1`.**
9. **Actions fijadas por SHA**, con la versión en un comentario. Dependabot las sigue
   actualizando.
10. **Matriz de acceso**: un test de integración que recorre todas las operaciones de
    `openapi.yaml` con seis identidades: sin sesión, alumno sin verificar, alumno gestionado,
    otro alumno, profesor y admin. Afirma el estado que cada una debe recibir, y sobre recursos
    del alumno gestionado, de modo que "otro alumno" prueba el acceso horizontal.
    - Una operación sin fila en la matriz rompe el build, igual que una ruta que no esté en el
      spec.
    - Los tests sueltos que ya existen se quedan: prueban la regla de cada módulo; la matriz
      prueba que ninguna ruta se escapa.

## Fuera, y por qué

- **Bloqueo o frenado por cuenta tras fallos de login.** Sigue fuera
  (`13-fase5-analisis-identity.md`). Con un solo profesor, cualquiera podría dejarle sin entrar
  con diez intentos. El riesgo lo acotan bcrypt, los 10 caracteres mínimos y el límite por IP,
  que ya funcionará.
- **Revocar el access token al desactivar una cuenta.** Se mantiene la decisión de la Fase 12:
  15 minutos como máximo.
- **Registro de eventos de seguridad** (logins fallidos, desactivaciones): Fase 16, con los logs
  estructurados.
- **HTTPS, el proxy de entrada y los secretos en el proveedor**: Fase 17.
- **Pentest externo**: no tiene sentido antes de desplegar.

## Criterios de aceptación

- `trivy` y `npm audit` en CI, en verde. Comprobado rompiéndolo: con Tomcat 10.1.55 el job
  falla.
- Hay un test que demuestra que con 21 peticiones desde el proxy, pero desde dos clientes
  distintos, ninguno recibe 429. Otro demuestra que un `X-Forwarded-For` que llega de fuera de
  la lista se ignora.
- Hay un test para cada estado de protocolo: una ruta desconocida da 404, `DELETE /me` da 405 y
  un `Content-Type` equivocado da 415, todos con su `code` y sin traza `ERROR`. Otro test
  demuestra que el `500` no filtra el mensaje de la excepción.
- Las cabeceras están en las respuestas del frontend. Hay un test E2E que lo comprueba y que la
  aplicación sigue funcionando con la CSP; la suite E2E completa es la prueba de que la CSP no
  rompe nada. Comprobado rompiéndolo: sin el nonce la página no carga.
- Hay tests del correo por dirección: el segundo en 5 minutos no se envía y la respuesta sigue
  siendo 202.
- Hay un test que demuestra que, tras un restablecimiento, el otro enlace pendiente responde
  `AUTH_INVALID_TOKEN`.
- La matriz de acceso cubre todas las operaciones del spec. Comprobado rompiéndolo: quitar la
  comprobación de propiedad de `POST /bookings/{id}/cancel` la hace fallar.
- `mvn verify`, la suite E2E y el frontend en verde, con 0 saltados.

## Decisiones tomadas al implementar

1. **Tomcat 10.1.60 y pgjdbc 42.7.13**, las últimas del momento y no el mínimo que corrige cada
   CVE: no tiene sentido fijar una versión que ya tiene sucesora.
2. **Trivy va como imagen de Docker fijada (`aquasec/trivy:0.74.0`)**, no como action de
   terceros: es una dependencia menos del CI.
3. **El reenvío de verificación sólo cuenta los reenvíos**, no el correo del registro. Si el
   primer correo no llega, quien pide otro al momento lo recibe.
4. **El límite de correos no cambia la respuesta ni evita crear el enlace**; sólo suprime el
   envío.
5. **En `X-Forwarded-For` sólo se compara una IP literal.** Resolver un nombre escrito por el
   cliente convertiría cada petición en una consulta DNS elegida por él.
6. **Los errores de protocolo (405, 406, 415) no se repiten en cada operación del spec.**
   `ContractValidation` exige que lleven el formato de error, y `11-contrato-api.md` los lista
   entre los códigos comunes.
7. **La matriz de acceso sólo ejecuta las escrituras que deben rechazarse.** Si ejecutara una
   escritura permitida, cambiaría los datos sobre los que corre el resto de la matriz, y de
   esas ya se ocupa el test de su módulo. Las lecturas permitidas sí se ejecutan y tienen que
   dar 2xx.
8. **Hallazgos que no estaban en el análisis**:
   - `main` se había quedado en la Fase 10 porque los PR encadenados se fusionaron en su rama
     base (#30). A partir de ahora, al fusionar un PR de una cadena, el siguiente se reapunta a
     `main`.
   - Una instalación vieja de Claude Code (WinGet) en el equipo de desarrollo. Ya está quitada;
     no afecta al proyecto.

