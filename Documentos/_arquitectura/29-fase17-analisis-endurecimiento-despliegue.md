# Fase 17 — Análisis: endurecimiento y despliegue

**Aparcada (30/09/2026).** Daniel no quiere pagar nada hasta tener el producto completo, y se
sigue en local. La parte de PWA y accesibilidad pasa a la Fase 30; lo demás espera tal cual, y
las decisiones que dice "tuyas" siguen abiertas.

Criterio de salida (`12-metodologia-trabajo.md`): PWA completa, accesibilidad, proveedor elegido
y política de backups. Es la última fase antes de que la usen alumnos de verdad. Por eso decide
dónde viven sus datos, cuánto tiempo y cómo se recuperan.

## Lo que otras fases dejaron para ésta

- **PWA**: instalable, con icono y manifest (01, 02, 22, 27). Hoy no hay manifest ni service
  worker; sólo `src/app/icon.svg`.
- **Accesibilidad**: la 15.5 cuidó el contraste, el foco, los 44 px y el movimiento reducido. La
  auditoría completa se dejó para aquí. Los E2E ya localizan por rol y nombre accesible.
- **Navegadores**: los E2E sólo usan Chromium. WebKit se dejó para esta fase, con la PWA (25).
- **HTTPS, proxy de entrada y secretos en el proveedor** (26). Requisito anotado: el proxy
  tiene que **sobrescribir** `X-Forwarded-For`, no añadirle.
- **Logs**: dónde se guardan y cuánto tiempo. La IP sigue fuera del log (28).
- **Métricas**: el exportador depende del proveedor (28).
- **Backups**: 01 los deja fuera del MVP como "copias automatizadas"; 12 pide una política.

Lo que ya está listo:

- el perfil `prod` falla al arrancar si falta una variable;
- la cookie de refresco es `Secure` por defecto;
- HSTS está puesto;
- los logs salen en JSON;
- el backend tiene imagen Docker sin root, y `trivy` la revisa.

El frontend no tiene imagen: los E2E lo arrancan con `next start`.

## Propuesta: dos subfases

La fase mezcla dos cosas que no dependen una de otra:

- **17, PWA y accesibilidad**: sólo código y tests. Puede empezar en cuanto valides esto.
- **17.1, despliegue y backups**: necesita que tomes antes las decisiones de abajo, y abrir
  cuentas que sólo puedes abrir tú.

Cada una con su rama, su PR y su tag. Así la primera no espera a que elijas proveedor.

## 17 — PWA y accesibilidad

1. **Manifest con `app/manifest.ts`**, la convención de Next 16. Lleva:
   - el nombre;
   - `display: standalone`;
   - el navy de la marca como `theme_color`;
   - iconos de 192 y 512 px, más una versión *maskable*, generados una vez desde `icon.svg` y
     versionados como PNG.
   - `start_url: /`, que ya lleva a cada rol a su inicio.
2. **Service worker propio y pequeño** (`public/sw.js`), sin librería. Serwist haría lo mismo con
   una dependencia y un paso de build más.
   - Cachea sólo lo estático: `/_next/static` e iconos.
   - Las páginas van por red. Sin conexión, sale una página "Sin conexión".
   - **Nunca cachea `/api`**: ni datos personales en el dispositivo, ni reservar sin red
     (`02-arquitectura.md` §13).
   - Se registra desde un componente cliente del layout, que va en el bundle con su nonce.
3. **La CSP gana `worker-src 'self'`.** Con `'strict-dynamic'`, el `'self'` de `script-src` no
   cuenta, y el navegador podría rechazar el worker en silencio. `manifest-src` hereda
   `default-src 'self'` y ya vale.
4. **Sin notificaciones push.** Son "notificaciones externas", fuera del MVP (01 §16), y
   necesitan claves VAPID y un servidor que las envíe.
5. **Auditoría con axe en los E2E.** `@axe-core/playwright` entra como **dependencia de
   desarrollo**, la única nueva de la fase. Cada pantalla de cada rol se analiza, y un fallo
   `serious` o `critical` rompe el job. Lo `moderate` se revisa y se arregla o se anota. Además,
   un recorrido a mano sólo con teclado por los flujos principales, con `playwright-cli`, a 390
   y 1440 px.
6. **WebKit para los flujos del alumno** (un iPhone), que es Safari en su móvil. Los de profesor
   y admin siguen en Chromium de escritorio. Firefox no aporta lo suficiente para doblar la
   suite.
7. **Lo que axe o el teclado encuentren se arregla en esta subfase**, sin cambiar lo que hacen
   las pantallas.

Skills: **frontend-design**, para los iconos y la página sin conexión dentro de la identidad de
la 15.5. **react-best-practices**: el registro del worker no bloquea la hidratación.
**playwright-cli**: el recorrido de teclado y la instalación a mano.

### Tests de la 17

- **E2E, instalable**: `/manifest.webmanifest` responde con los campos y los iconos existen.
- **E2E, sin conexión**: con el worker activo y la red cortada, una página muestra "Sin
  conexión"; una llamada a `/api` falla, y no sale de caché.
- **E2E, CSP**: el test de seguridad sigue sin violaciones, ahora también con el worker.
- **E2E, axe**: cero `serious`/`critical` en todas las pantallas. Se rompe quitando una
  etiqueta.
- **Unitario**: la lista de lo que el worker cachea no incluye nada de `/api`. Se rompe
  añadiéndolo.

## 17.1 — Despliegue y backups

### Decisiones que son tuyas

1. **Proveedor.** Mi recomendación es **un VPS pequeño en la UE** (Hetzner, unos 5 €/mes) con la
   misma composición de Docker que ya usamos, detrás de **Caddy**. Razones:
   - Hay una instancia y una pista. No hace falta escalar, y la app ya está hecha para una sola
     máquina: el rate limiting y los correos van en memoria.
   - Caddy saca y renueva el certificado HTTPS solo. Además, desde la 2.5 **ignora el
     `X-Forwarded-For` que manda el cliente** y pone el suyo, que es el requisito de la Fase
     15.
   - Los datos de los alumnos se quedan en la UE (RGPD).
   - El precio es: el sistema operativo lo mantienes tú (actualizaciones automáticas,
     cortafuegos, SSH con clave).

   La alternativa es un PaaS (Render, Railway, Fly): menos mantenimiento, Postgres gestionado
   con backups, y entre 20 y 40 €/mes con dos servicios y la base.
2. **Dominio.** HTTPS lo necesita. Puede ser uno propio o un subdominio gratuito.
3. **Correo.** `prod` exige un SMTP con usuario y contraseña. Propuesta: un proveedor
   transaccional con servidores en la UE y plan gratuito (Brevo o Mailjet). Da de sobra para
   verificaciones y restablecimientos.
4. **Retención.** Propuesta:
   - backups diarios, guardados **14 días**;
   - logs, **14 días** en el propio servidor, con rotación de Docker.
   Son datos personales: se guardan lo justo para recuperarse de un error y no más.

### Decisiones propuestas

5. **Backups sí, automáticos, pese a 01 §16.** Perder las reservas de todos es peor que un
   `cron`, y cuesta poco. Se corrige 01. La política:
   - un `pg_dump` diario lo hace un servicio de la composición;
   - se **cifra** con `age` antes de salir del servidor;
   - se copia **fuera** del servidor, a almacenamiento de objetos en la UE;
   - se borra al cumplir la retención.
   - Un `restore.sh` lo devuelve a una base vacía. **Se ensaya una vez antes de abrir**, con la
     evidencia pegada: un backup que no se ha restaurado nunca no es un backup.
6. **Imagen del frontend** con `output: "standalone"`, sin root como la del backend, y también
   revisada por `trivy`.
7. **`compose.prod.yaml`**, aparte del de desarrollo:
   - postgres, backend (`prod`), frontend, Caddy y el backup;
   - sólo Caddy publica puertos (80 y 443); la base y el backend no salen de la red interna;
   - sin Mailpit;
   - límites de memoria y `server.shutdown: graceful`.
   - Los secretos van en un `.env` del servidor con permisos `600`, nunca en Git. El
     `JWT_SECRET` se genera aleatorio, de 64 bytes.
8. **El backend sólo confía en el frontend** como proxy (la red interna de la composición). El
   frontend sólo recibe de Caddy, que sobrescribe la cabecera. Así la IP del rate limiting es la
   real.
9. **Despliegue a mano, con un script**: `deploy.sh <tag>` en el servidor hace checkout del tag,
   construye y reinicia. Con una persona y un tag por fase, automatizarlo desde CI añade secretos
   de SSH en GitHub y no ahorra nada.
10. **Métricas sin exportador.** `/actuator` no sale por Caddy; se consulta por un túnel SSH. Un
    Prometheus para una instancia es más de lo que mide.
11. **CI arranca también la composición de producción**, con valores de prueba, y comprueba el
    health y una página. El perfil `prod` no tiene valores por defecto, y así una variable
    nueva que falte se ve en el PR, no al desplegar.
12. **Un documento de operación** (`30-operacion.md`), corto: desplegar, restaurar, rotar un
    secreto y dónde mirar los logs. Es lo único que se consulta con prisa.

### Lo que haces tú

- Abrir las cuentas y pagar: el proveedor, el dominio, el correo y el almacenamiento.
- Apuntar el DNS.
- Crear el `.env` del servidor con los secretos. No los veo ni pasan por el repositorio.

Yo preparo todo lo demás y te acompaño en el primer despliegue, paso a paso.

### Tests de la 17.1

- **CI**: la composición de producción arranca, y responden `/actuator/health` y `/login`.
- **Backup**: un test del script restaura un dump en un Postgres vacío y encuentra las tablas.
  Se rompe corrompiendo el fichero.
- **En el servidor, antes de abrir**, con evidencia pegada:
  - HTTPS válido, con HSTS;
  - la base no responde desde fuera;
  - un `X-Forwarded-For` falso no cambia la IP que cuenta el rate limiting;
  - un backup restaurado.

## Fuera, y por qué

- **Notificaciones push**: fuera del MVP (01 §16).
- **Réplicas, alta disponibilidad y staging aparte**: una instancia basta. La composición de
  producción en CI hace de ensayo.
- **Colector de logs y paneles**: con una instancia, `docker compose logs` y el correlation id
  bastan. Se revisa si hace falta.
- **Errores del navegador (tipo Sentry)**: sería otro servicio y otra entrada en la CSP. Hoy un
  500 ya enseña su código de referencia.
- **Textos legales** (privacidad, aviso legal): hacen falta antes de abrir a alumnos reales,
  pero el texto no es código. La fase deja sitio para las páginas; el contenido lo pones tú.

## Criterios de aceptación

**17:**
- La app se instala en Android y en iOS, lo que se comprueba a mano con capturas, y el test del
  manifest pasa.
- Existe un test que demuestra que `/api` nunca sale de la caché del worker.
- axe da cero `serious`/`critical` en todas las pantallas, y se ha visto fallar.
- Los E2E del alumno pasan en Chromium y en WebKit.
- CI en verde, sin saltados.

**17.1:**
- La composición de producción arranca en CI.
- Un backup se ha restaurado, en test y en el servidor.
- La app está en su dominio con HTTPS, y los tres controles del servidor están pegados.

## Orden de trabajo

**17:** manifest e iconos → worker y página sin conexión → CSP → axe y arreglos → WebKit.

**17.1** (tras tus decisiones): imagen del frontend → `compose.prod.yaml` y Caddy → backup y
restauración → CI → documento de operación → primer despliegue contigo.
