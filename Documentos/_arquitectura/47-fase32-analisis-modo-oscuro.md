# Fase 32 — Análisis: modo oscuro de la app

Criterio de salida propuesto (`12-metodologia-trabajo.md`): todas las pantallas de los tres roles en
claro y en oscuro, con la misma elección que la web, y axe sin fallos graves en los dos temas.

La Fase 30 lo dejó fuera (decisión 1, Daniel, 03/11/2026) para hacerlo con su propia maqueta. La
maqueta acompaña a este análisis y usa exactamente los colores que se proponen aquí; Daniel la
validó con las tres decisiones el 09/11/2026.

## Punto de partida

- **La web ya tiene los dos temas** (Fase 31, decisión 8): `[data-site]` con tokens `--site-*`
  escritos con `light-dark()`, la cookie `THEME_COOKIE` que pone `data-theme` en el servidor y un
  botón que la cambia. Sin cookie, manda el sistema. El comentario de `globals.css` ya dice que esos
  valores oscuros son para la app, no una segunda paleta.
- **La app está entera en tokens.** 72 ficheros usan los colores de `@theme` (`paper`, `ink`, `muted`,
  `line`, `court`…) y ninguno usa la paleta de Tailwind (`gray-*`, `blue-*`) ni un hexadecimal; el
  único es `BRAND_NAVY`, para el manifest. Cambiar el valor de los tokens recolorea casi todo.
- **El «casi» son los tokens con dos papeles.** El mismo color sirve de fondo con texto blanco y de
  texto sobre blanco. En claro funciona; en oscuro, no:
  - `court`: fondo del botón principal, de la tarjeta de la próxima clase y de los bloques de grupo
    de la agenda (`bg-court`, con `text-white`), y texto, borde y acento sobre papel (`text-court`,
    `border-court`, `accent-court`, `fill-court`, `stroke-court`: 33 ficheros). Un azul que lleve
    texto blanco es demasiado oscuro para ser texto sobre azul noche.
  - `navy`: el lateral, la pantalla de entrar, el logo, los avatares, las plazas ocupadas y «Faltó»
    son fondo; el botón secundario lo usa de texto y borde (`text-navy`, `border-navy`).
  - `fault`: texto de error (8 usos) y fondo del botón de peligro al pasar por encima (`bg-fault`).
- **El logo pinta el pico en `fill-navy`.** Sobre la página oscura desaparece; ya tiene la variante
  `onDark`, con un borde blanco, que usa el lateral.
- **axe corre en todas las pantallas de la app** (Fase 30), en claro: el alumno a 390 px, profesor y
  admin a 1440 px. La web ya corre en los dos temas (`e2e/website.spec.ts`).
- **La página sin conexión** (`public/offline.html` y `offline.css`) es estática y sólo clara.
- **La barra del sistema y el arranque de la app instalada van en navy**, que sirve en los dos temas.

## Decisiones propuestas

### 1. Quién elige el tema — **A** (Daniel, 09/11/2026)

- **A (te la recomiendo). Sigue al sistema, y se puede fijar en el Perfil.** Un apartado
  «Apariencia» con tres opciones: «Como el móvil», «Claro» y «Oscuro». Es la misma cookie que la
  web: quien eligió oscuro en la portada entra en la app en oscuro, y al revés. «Como el móvil»
  borra la cookie. Así no hay un botón más en la cabecera del móvil, que ya lleva «Ir a la web».
- **B. Sólo el sistema.** Nada que elegir; quien quiera la app clara con el móvil oscuro no puede.
- **C. Un botón en el marco de la app**, como en la web: en la cabecera del móvil y en el lateral.
  Más a mano, pero es un control que se toca una vez y ocupa sitio en todas las pantallas.

### 2. Las superficies de la marca en oscuro — **A** (Daniel, 09/11/2026)

Son la tarjeta de la próxima clase (alumno y «Hoy»), el botón principal, los bloques de grupo de la
agenda, el día de hoy en la agenda y «Tienes plaza».

- **A (te la recomiendo). Siguen en azul pista con texto blanco**, un punto más vivo (`#2864c0`)
  para que se separen de la página. La tarjeta de la pista sigue siendo el único gesto fuerte de la
  pantalla, igual que en claro, y el texto blanco pasa AA.
- **B. Azul claro con texto navy**, como hacen muchas apps en oscuro. Separa más del fondo, pero la
  tarjeta de la pista se vuelve pálida y deja de parecerse a la de la web.

### 3. El lateral del profesor y del admin en oscuro — **A** (Daniel, 09/11/2026)

- **A (te la recomiendo). Un navy más hondo que la página** (`#06112a`, el de las bandas de la web)
  con una línea a la derecha. El marco se sigue distinguiendo del contenido, como en claro.
- **B. Del mismo color que las tarjetas.** El lateral se confunde con el contenido.

## Reglas (se desarrollan de las decisiones y de la Fase 31, no se preguntan)

1. **Una paleta, no dos.** Los tokens de `@theme` toman en oscuro los valores de la web donde
   coinciden (página `#0a1730`, superficie `#0f2347`, texto `#eef2f7`, apagado `#a9b8d0`, enlace
   `#8fb3e6`). Los `--site-*` de la web no se tocan en esta fase.
2. **Cada token tiene un solo papel.** Los tres que tienen dos se separan:

   | Hoy | Queda | Papel | Claro | Oscuro |
   |---|---|---|---|---|
   | `court` como fondo | `court` | fondo con texto blanco | `#1f5fb8` | `#2864c0` |
   | `court-deep` | `court-deep` | ese fondo al pasar por encima | `#174a91` | `#2558a8` |
   | `court` como texto, borde, acento | `link` (nuevo) | texto, borde, casillas sobre la superficie | `#1f5fb8` | `#8fb3e6` |
   | `navy` como fondo | `navy` | relleno sólido: avatar, plaza ocupada, «Faltó» | `#0b1e3f` | `#3a5687` |
   | `navy` del marco | `frame` (nuevo) | lateral y pantalla de entrar | `#0b1e3f` | `#06112a` |
   | `navy` del logo | `brand` (nuevo) | pico y ojo del logo | `#0b1e3f` | `#0b1e3f` |
   | `navy` como texto | `ink` | botón secundario | `#0b1e3f` | `#eef2f7` |
   | `fault` como fondo | `fault-fill` (nuevo) | botón de peligro al pasar por encima | `#b42318` | `#c0392b` |

   El resto cambia sólo de valor:

   | Token | Claro | Oscuro |
   |---|---|---|
   | `ground` (página) | `#eef2f7` | `#0a1730` |
   | `paper` (tarjetas, tablas, formularios) | `#ffffff` | `#0f2347` |
   | `ink` | `#0b1e3f` | `#eef2f7` |
   | `muted` | `#4e5d78` | `#a9b8d0` |
   | `line` | `#d3dbe7` | `#2c4570` |
   | `line-soft` | `#e3e9f1` | `#1a3058` |
   | `court-tint` | `#e3ecf9` | `#1b3a6b` |
   | `on-navy` | `#c9d6ea` | `#c9d6ea` |
   | `surround` (fondo, texto blanco) | `#1f7a55` | `#1f7a55` |
   | `surround-tint` / `surround-ink` | `#d7efe3` / `#1f5f43` | `#16402f` / `#a8e3c6` |
   | `warning-tint` / `warning` | `#fcebd2` / `#7a4300` | `#43300f` / `#f2c27b` |
   | `fault-tint` / `fault` (texto) | `#f7dcd6` / `#b42318` | `#4a1d19` / `#ff9b8c` |
   | `ball` | `#e4f03c` | `#e4f03c` |

   Todos los pares de texto pasan AA en los dos temas; el más justo en oscuro es el blanco al 85 %
   sobre `court` (4,6:1). La tabla completa de contrastes está en la maqueta.
3. **Variables redefinidas, no `light-dark()`.** `@theme` define el claro; un bloque para
   `[data-theme="dark"]` y otro para `prefers-color-scheme: dark` sin `data-theme="light"` lo
   sobrescriben. `light-dark()` llegó a Safari en iOS 17.5: en un iPhone anterior, la declaración se
   descarta entera y la app se quedaría sin colores. Con variables, ese iPhone ve el claro. La web
   sigue con `light-dark()`, que es su decisión y su riesgo.
4. **El tema lo pone el servidor en `<html>`**, desde la cookie, en el layout raíz: llega ya en sus
   colores, sin script antes de hidratar (la CSP con nonce tendría que dejarlo pasar) y sin parpadeo
   del otro tema. `color-scheme` va con él, para que los controles nativos (fechas, selects, barras
   de desplazamiento) sigan el tema. La web deja de ponerlo en su `div` y lo lee del mismo sitio.
5. **El logo pinta con `brand`**, y su borde blanco del pico se enciende con el tema oscuro: deja de
   hacer falta pasar `onDark` a mano.
6. **El rayado de la agenda** (clases canceladas, fuera de horario) ya usa `ground` y `line-soft`, y
   sigue al tema solo. El texto sobre él pasa AA en los dos.
7. **Las fotos de los alumnos no cambian.** Un avatar sin foto es `navy` con texto blanco.
8. **La página sin conexión sigue al sistema** con `prefers-color-scheme` en `offline.css`: es
   estática y no lee la cookie. Cambiar el CSS pide subir el `VERSION` de `sw.js`.
9. **Los correos no cambian.** Cada cliente de correo decide su tema, y aquí no hay nada que ganar.
10. **La barra del sistema y el arranque siguen en navy** (`BRAND_NAVY`), en los dos temas.

## Contrato

Ninguno: la fase es sólo frontend. `openapi.yaml` no cambia. La preferencia vive en la cookie del
navegador, como en la web, no en la cuenta: no es un dato que el servidor necesite.

## Fuera, y por qué

- **Guardar el tema en la cuenta**, para que salga igual en todos los dispositivos: un campo y un
  endpoint para algo que cada móvil ya sabe decidir.
- **Unificar los `--site-*` de la web con los tokens de la app**: la web tiene bandas, mapa y
  horario propios. Se puede hacer después, con los tokens de la app como base.
- **Un tema de alto contraste**: nadie lo ha pedido, y el claro y el oscuro ya pasan AA.

## Tests

- **E2E, axe en los dos temas**: `accessibility.spec.ts` y `accessibility.student.spec.ts` corren
  en claro y en oscuro, como `website.spec.ts`. Cero `serious`/`critical` en todas las pantallas de
  los tres roles y en las de entrar. Se rompe poniendo `muted` de oscuro en `#4e5d78`.
- **E2E, el servidor pone el tema**: con la cookie en oscuro, el HTML de `/home` llega con
  `<html data-theme="dark">` antes de hidratar. Sin cookie no lleva `data-theme`, y con el sistema
  en oscuro el fondo de la página es `#0a1730`.
- **E2E, la elección del Perfil**: elegir «Oscuro» cambia el tema al momento y sigue tras recargar;
  «Como el móvil» vuelve al sistema. Lo elegido en la web se ve en la app.
- **Unitario, contraste de la paleta**: lee los tokens de `globals.css` de los dos temas y comprueba
  cada par de texto sobre su fondo (≥ 4,5:1). Cubre los estados que axe no llega a ver en una
  pantalla. Se rompe bajando `muted`.
- **E2E del profesor**: la agenda sigue colocando los bloques en su fila en los dos temas.
- **Unitario del worker**: sigue igual; el `VERSION` sube por `offline.css`.

## Criterios de aceptación

- Las 19 pantallas de la app y las 5 de entrar se ven en claro y en oscuro (capturas en el PR, a
  390 y a 1440 px), y existe un test que demuestra que el tema llega del servidor.
- axe da cero `serious`/`critical` en los dos temas, y se ha visto fallar.
- El test de contraste de la paleta pasa, y se ha visto fallar.
- La elección del Perfil se comparte con la web.
- CI en verde, sin saltados.

## Riesgos

- **Un color fuera de los tokens** en una pantalla poco visitada. La búsqueda de hoy no encuentra
  ninguno, y la regla de lint de la Fase 30 (o una nueva) puede prohibir `text-white` fuera de los
  fondos de marca; se decide al implementar si hace falta.
- **El CI tarda más**: los dos specs de axe corren dos veces. Son de los más rápidos.
- **Renombrar `text-court` a `text-link`** toca 33 ficheros. Es mecánico, y lo cubren axe y los E2E.

## Orden de trabajo

Tokens y bloque oscuro en `globals.css` → tema en `<html>` desde la cookie → separar los tokens de
dos papeles → logo → Perfil «Apariencia» → página sin conexión → axe en los dos temas y arreglos →
test de contraste → capturas → cierre.

## Decisiones tomadas al implementar

- **El azul de la pista en oscuro es `#2864c0`, no el `#2e6dcc` que se propuso.** axe lo encontró en
  la primera pasada en oscuro: las segundas líneas de la tarjeta de la próxima clase van en blanco al
  85-90 %, y sobre `#2e6dcc` se quedaban en 4,1:1. Con `#2864c0` dan 4,6:1, y el blanco entero 5,7:1.
  Sigue un punto más vivo que el del claro, y la maqueta ya lo lleva. El test de la paleta comprueba ahora también ese blanco
  translúcido.
- **El tema vive en `<html>` para la web y la app**: el layout raíz lo pinta desde la cookie, y la
  web dejó de ponerlo en su `div`. El botón de la web y «Apariencia» usan las mismas funciones
  (`src/shared/theme.ts`, que sale de la web porque ya no es sólo suyo). `color-scheme` va en
  `:root`, así que los `--site-*` con `light-dark()` lo heredan sin cambiar.
- **Los tokens oscuros van en `@layer theme`**, la capa donde Tailwind escribe los de `@theme`: así
  ninguna utilidad pierde contra ellos por estar fuera de capa.
- **La web usaba `navy` como texto sobre el amarillo** (botón de la clase de prueba, «¿Qué clase es
  para mí?») y en el mapa. Pasa a `brand`, que es navy en los dos temas, y la web no cambia.
- **«Apariencia» sale en el Perfil de los tres roles**, debajo de lo propio de cada uno. Hasta que
  el navegador dice qué hay elegido, no marca ninguna opción: el servidor no pinta la página del
  Perfil con la cookie.
- **La política de privacidad nombra la cookie** también para la app.
- **El rayado de la agenda en oscuro contrasta más que en claro** (1,4:1 contra 1,1:1): es el mismo
  `line-soft`, que en oscuro tiene que separarse de las líneas de la rejilla. Se dejó así, como en la
  maqueta.
- **WebKit no se pudo correr en la sesión** (la descarga del navegador está bloqueada): sus
  resultados son los del CI. Los 63 E2E de Chromium pasan contra el stack local.
