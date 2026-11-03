# Fase 30 — Análisis: PWA y accesibilidad

Criterio de salida (`12-metodologia-trabajo.md`): instalable, sin conexión sin datos personales,
axe sin fallos graves y WebKit en los flujos del alumno.

La Fase 17 la diseñó y la aparcó (`29-fase17-analisis-endurecimiento-despliegue.md`, «17 — PWA y
accesibilidad»). Este análisis parte de aquel y lo pone al día con lo que ha cambiado desde
entonces: la web pública vive en `/` (Fase 31), la app se llama Oasis Papagayo y ya tiene doce
fases más de pantallas.

## Punto de partida

- **No hay manifest ni service worker.** Sólo `src/app/icon.svg`.
- **La CSP lleva nonce y `'strict-dynamic'`** (`src/proxy.ts`). Con `'strict-dynamic'` el `'self'`
  de `script-src` no cuenta, así que un worker sin `worker-src` se rechazaría en silencio.
- **`/` es la web, no la app.** La app instalada no puede arrancar ahí: abriría la portada.
- **Cada rol tiene su inicio** (`homeFor`), y `RequireAccount` manda a cada uno al suyo: quien
  entra en `/home` sin ser alumno acaba en `/teacher` o `/admin`, y sin sesión en `/login`.
- **axe ya corre en la web** (`e2e/website.spec.ts`, Fase 31), en claro y oscuro, a 390 y 1440 px,
  y falla con `serious`/`critical`. En la app no corre en ninguna pantalla.
- **Los E2E sólo usan Chromium**: el alumno con un Pixel 7, profesor y admin en escritorio. El CI
  instala sólo `chromium`.
- **La app no tiene modo oscuro**; la web sí. La Fase 31 lo apuntó para esta fase.

## Decisiones propuestas

### 1. Modo oscuro de la app — **A** (Daniel, 03/11/2026)

- **A (te la recomiendo). Fuera de esta fase.** Es repasar el color de todas las pantallas de los
  tres roles, y nada del criterio de salida lo pide. Con la PWA, el fondo y la barra del sistema
  van en el azul de la marca, que funciona igual en claro y en oscuro. Se puede hacer después con
  su propia maqueta.
- **B. Dentro.** La fase dobla su tamaño, y axe tendría que correr en los dos temas en toda la app.

### 2. Cómo se le explica al alumno que puede instalarla — **A** (Daniel, 03/11/2026)

Android (Chrome) ofrece instalarla solo. En iPhone hay que ir a «Compartir → Añadir a pantalla de
inicio», y Safari no avisa.

- **A (te la recomiendo). Una línea en su perfil, sólo si no la tiene instalada:** «Instálala en tu
  móvil» con los dos pasos de su sistema. Sin ventanas emergentes.
- **B. Nada.** Marcos se lo explica en persona.

### 3. Notificaciones push

**Fuera**, como decidió la 17: son notificaciones externas (`01` §16), piden claves VAPID y un
servidor que las envíe, y en iPhone sólo funcionan con la app instalada. Los avisos siguen por correo
y en el tablón.

## Reglas (se desarrollan del criterio y de `02` §13, no se preguntan)

1. **Manifest con `app/manifest.ts`**, la convención de Next 16:
   - `name` «Oasis Papagayo», `short_name` «Oasis», `lang: es`;
   - `display: standalone`, `theme_color` y `background_color` en el azul de la marca;
   - **`start_url: /home`**, que lleva a cada rol a su inicio y a `/login` sin sesión;
     `scope: /`, para que los enlaces a la web no salgan de la app;
   - iconos PNG de 192 y 512 px y uno *maskable*, generados una vez desde `icon.svg` y
     versionados.
2. **iPhone no lee los iconos del manifest**: `app/apple-icon.png` (180 px) y `appleWebApp` en los
   metadatos, con la barra de estado sobre el azul. `theme-color` va en el `viewport` del layout.
3. **Service worker propio y pequeño** (`public/sw.js`), sin librería:
   - cachea sólo lo estático: `/_next/static`, iconos, fuentes y la página sin conexión;
   - las páginas van siempre por red; sin red, sale la página «Sin conexión»;
   - **nunca cachea `/api`** ni una página de la app: en el dispositivo no queda ningún dato
     personal, y no se reserva ni se cancela sin red (`02` §13);
   - la caché lleva el nombre de la versión y la vieja se borra al activarse la nueva.
4. **La página sin conexión es HTML estático** (`public/offline.html`), sin scripts y con su hoja
   de estilos en un fichero: así no depende del nonce, que cambia en cada petición. Dice que no hay
   conexión, que las reservas necesitan red, y tiene un botón «Reintentar» que es un enlace.
5. **El worker se registra desde un componente cliente del layout de la app**, después de la
   hidratación, y sólo en producción: en `next dev` cachearía lo que está cambiando.
6. **La CSP gana `worker-src 'self'` y `manifest-src 'self'`.** El matcher de `src/proxy.ts` deja
   fuera `sw.js`, el manifest, los iconos y `offline.html`, como ya hace con `icon.svg`.
7. **Al cerrar sesión no queda nada que borrar**, porque nada personal se cachea. El test lo
   comprueba igual (abajo).
8. **axe en todas las pantallas de la app**, cada rol con su tamaño: el alumno a 390 px, profesor y
   admin a 1440 px, y las de entrar y registrarse a los dos. Un `serious` o `critical` rompe el job;
   lo `moderate` se arregla o se anota aquí con su porqué.
9. **Un recorrido sólo con teclado** por los flujos principales de los tres roles, a mano con
   `playwright-cli`: entrar, reservar, cancelar, lista de espera, comprar un bono; crear clase,
   pasar lista, marcar un objetivo; cambiar el estado de un alumno. Lo que salga se arregla sin
   cambiar lo que hace la pantalla.
10. **WebKit para los flujos del alumno**: un proyecto `mobile-webkit` con un iPhone que corre los
    mismos `student` y `navigation` que el Pixel 7. Profesor y admin siguen en Chromium de
    escritorio; Firefox no aporta lo bastante para doblar la suite.

## Contrato

Ninguno: la fase es sólo frontend y CI. `openapi.yaml` no cambia.

## Fuera, y por qué

- **Modo oscuro de la app** y **push**: decisiones 1 y 3.
- **Ver las clases sin conexión**: serían datos personales en el dispositivo, que es justo lo que
  el criterio prohíbe.
- **Un botón propio de «Instalar»**: Chrome ya lo ofrece, y en iPhone no existe esa API.
- **Lighthouse como puerta del CI**: la Fase 31 ya lo midió en la web; aquí se mide una vez la app
  instalable y se pega el resultado.

## Tests

- **E2E, instalable**: `/manifest.webmanifest` responde con `start_url`, `display` y los iconos, y
  cada icono responde 200 con su tamaño.
- **E2E, sin conexión**: con el worker activo y la red cortada, una página de la app muestra «Sin
  conexión», y una llamada a `/api` falla en lugar de salir de la caché.
- **E2E, nada personal en caché**: tras recorrer las pantallas del alumno con sesión, ninguna
  entrada de `caches` es de `/api` ni de una página de la app. Se rompe cacheando una.
- **E2E, CSP**: `security.spec.ts` sigue sin violaciones, ahora con el worker registrado.
- **E2E, axe**: cero `serious`/`critical` en todas las pantallas de la app. Se rompe quitando la
  etiqueta de un campo.
- **Unitario**: la lista de lo que el worker cachea no incluye `/api` ni rutas de la app. Se rompe
  añadiendo una.
- **WebKit**: los E2E del alumno en verde en WebKit, en CI, sin saltados.

## Criterios de aceptación

- La app se instala en Android y en iPhone y arranca en el inicio de cada rol (comprobado a mano,
  con captura).
- Sin red sale «Sin conexión», y existe un test que demuestra que nada de `/api` ni de las páginas
  queda en caché.
- axe da cero `serious`/`critical` en todas las pantallas de la app, y se ha visto fallar.
- Los E2E del alumno pasan en Chromium y en WebKit.
- CI en verde, sin saltados.

## Riesgos

- **WebKit y la cookie de refresco.** Es `Secure`; en `http://localhost` Chromium la acepta y WebKit
  puede no hacerlo. Si pasa, el E2E levanta el stack sin `Secure`, como ya hacen sus claves de
  prueba, nunca el perfil `prod`.
- **El CI tarda más**: la suite del alumno corre dos veces, y hay que instalar WebKit con sus
  dependencias.
- **La caché del worker y un despliegue nuevo**: los ficheros de `/_next/static` llevan hash, así que
  uno viejo no se mezcla con uno nuevo; aun así, el test de versión borra la caché anterior.

## Orden de trabajo

Manifest e iconos → página sin conexión y worker → CSP → axe en la app y arreglos → recorrido con
teclado → WebKit en local y en CI → instalación a mano en Android e iPhone → cierre.

## Decisiones tomadas al implementar

- **Los iconos se generan una vez desde `icon.svg`** con Chromium y se versionan: `public/icons/`
  (192, 512 y 512 *maskable*, la marca al 80 % sobre el azul para la zona segura) y
  `src/app/apple-icon.png` (180), que Next enlaza como `apple-touch-icon`.
- **El worker se registra en `Providers`, no sólo en el layout de la app**: el alumno llega
  primero a `/login`, y desde ahí ya queda la página sin conexión. En la web no cambia nada,
  porque las páginas siguen yendo por red.
- **`sw.js` lleva un `VERSION` a mano**: lo de `/_next/static` lleva hash, y sólo cambiar
  `offline.html`, su CSS o los iconos pide subirlo. `/sw.js` sale con `Cache-Control: no-cache`.
- **La barra de estado de iPhone es `default`** (clara, con la página debajo): el marco de la app es
  claro, y con `black-translucent` la cabecera quedaría bajo la hora.
- **El aviso de instalación sólo sale en Android y en iPhone/iPad** (un iPad dice ser un Mac, y se
  le reconoce por la pantalla táctil). En un ordenador no sale.
- **Lo que axe encontró, arreglado** (todo `serious`, salvo lo último):
  - *label-content-name-mismatch*: el logo de la cabecera del móvil se llamaba «Inicio» y decía
    «Oasis Papagayo». Ahora se llama como lo que dice.
  - *color-contrast*: las clases canceladas y terminadas de la agenda se apagaban con `opacity-70`,
    y el rayado se veía a través del texto. Ahora se apagan con colores (gris sobre gris claro, o
    gris sobre blanco) que pasan AA.
  - *heading-order* (`moderate`): los días de la semana eran `h3` justo bajo el `h1`. Ahora `h2`.
    Ningún `moderate` queda abierto.
- **La comprobación de axe espera a que la pantalla no diga «Cargando…»**, para que mire el
  contenido y no el esqueleto (`e2e/support/axe.ts`, que ahora usa también la web).
- **Las pruebas de sin conexión y de caché corren en Chromium**: son del worker, no de un
  navegador; en WebKit corren los flujos del alumno, su axe y el aviso de instalación.
- **El recorrido con teclado se hizo con un script de Playwright, no a mano** (sin `playwright-cli`
  en la sesión): el alumno entra y reserva sólo con el teclado, y en las 19 pantallas de los tres
  roles, a 390 y a 1440 px, cada elemento que recibe el foco se ve con su contorno y ninguno está
  oculto. No encontró nada; no se versiona, porque axe y los E2E ya cubren lo que podría romperse.
- **WebKit no se pudo correr en la sesión** (la descarga del navegador está bloqueada): sus
  resultados son los del CI.
- **El E2E levanta el backend con la cookie de refresco sin `Secure`** (`compose.e2e.yaml`). Pasó lo
  que decía el riesgo: en el primer CI con WebKit, el iPhone perdía la sesión en cada recarga y
  cuatro flujos del alumno fallaban. Chromium guarda la cookie `Secure` en `localhost` y WebKit no.
  El perfil `prod` sigue con `Secure`.
- **El axe de una pantalla comprueba antes que sigue en esa pantalla**: con la sesión perdida, WebKit
  acababa en `/login` y axe miraba el login, así que el test del alumno pasaba sin mirar sus
  pantallas.
- **Salir y volver a entrar espera a la página de login** (`signOut` en `e2e/support/fixtures.ts`).
  La app va sola a `/login` al salir. Si el test navega antes de que llegue, Chromium lo deja pasar
  y WebKit corta la navegación. Lo usan todos los tests que salen.
