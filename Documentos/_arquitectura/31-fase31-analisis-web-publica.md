# Fase 31 — Análisis: web pública

Criterio de salida (`12-metodologia-trabajo.md`): quien llega a `/` sin cuenta entiende qué es
la Escuela de Tenis Oasis Papagayo, cuánto cuesta y cómo empezar, y llega a Marcos con un toque.
La app entera lleva la marca nueva. Sólo frontend: el contrato y el backend no cambian.

Referencia visual: la dirección A ("Pista"), en claro y en oscuro, de la maqueta aprobada por
Daniel (https://claude.ai/artifact/5UVufJgAgrkdo6fyaeVP6J). El logo es el concepto 01 ("Pelota
con pico", cresta azul) de https://claude.ai/artifact/Drq488dwCcXf7en5M5fdxW.

## Punto de partida

- `/` no tiene página: `app/page.tsx` manda a `/login` o al inicio de cada rol.
- La marca es MAS Tennis Academy (`shared/ui/Logo.tsx`, `app/layout.tsx`,
  `app/(public)/layout.tsx`, `app/icon.svg`). La paleta de `globals.css` se queda.
- No hay modo oscuro en ninguna pantalla.
- Sin sesión, la API sólo deja registrarse, entrar y recuperar la contraseña: no hay datos
  públicos de clases ni de plazas.
- La cookie de refresco viaja sólo a `/api/v1/auth`: el servidor de Next no sabe, al pintar `/`,
  si quien pide la página tiene sesión.

## Lo que enseñaron otras webs

Se revisaron 13 (academias y escuelas de España, Latinoamérica, EE. UU. y Reino Unido, y una de
pádel). Lo que decide esta propuesta:

- Sólo 4 publican precios, y pocas muestran al profesor. Aquí el producto es Marcos y una pista:
  las dos cosas van a la vista.
- Convierte un único botón repetido hacia la clase de prueba. Las webs sin él son las más flojas.
- "Cómo empezar" y las preguntas frecuentes resuelven las dudas antes de escribir.
- Se evitan las fotos de banco de imágenes, los contadores ("60+ alumnos"), las noticias, los
  patrocinadores, las galerías y los precios en PDF.

La web pública de la maqueta de la Fase 15.5 se descartó por prometer lo que la app no tiene
(bono, lista de espera, horario con plazas en vivo, reserva de la prueba en la web), por datos
inventados con apariencia de reales y por un formulario que guardaba datos personales sin
backend.

## Decisiones

1. **Marca.** Escuela de Tenis Oasis Papagayo. El logo es una pelota con pico, ojo y cresta
   azul, como SVG en línea en `shared/ui/Logo.tsx`: el símbolo solo, o con el nombre
   ("Oasis Papagayo" en Barlow Condensed y "Escuela de tenis" debajo). Sobre fondo oscuro, el
   pico lleva un borde claro, o se pierde contra el navy. Cambian también `app/icon.svg`, el
   título de la app y la cabecera de las pantallas de acceso. Los colores no cambian: el
   amarillo pelota `#E4F03C` del logo se añade como token y sólo se usa en la marca y en el
   botón de la clase de prueba, nunca como texto sobre claro.
2. **Una página en `/`, para todos.** Sustituye a la redirección de `app/page.tsx`. No se
   redirige a quien tiene sesión: el servidor no puede saberlo sin cambiar la ruta de la cookie,
   y redirigir desde el cliente haría parpadear la web. En la cabecera, "Entrar" pasa a ser
   "Mi cuenta" (lleva a `homeFor(rol)`) cuando la sesión se restaura. Las pantallas de acceso
   siguen en `(public)`; la web va en su propio grupo, `app/(web)`, con su layout. Sus piezas
   viven en `app/(web)/_parts`: no son un módulo del backend, así que no van a `modules/`.
3. **Secciones**, en este orden: cabecera, portada, Clases (Niños, Adultos, Particulares),
   Cómo empezar (tres pasos), Marcos, Precios, Preguntas, Dónde y pie. Las opiniones no entran
   hasta que haya al menos tres reales.
4. **La clase de prueba se pide por WhatsApp** (`https://wa.me/<número>?text=…`, con el texto
   ya escrito). No hay formulario: registrarse no da derecho a reservar (el alumno tiene que
   estar gestionado por Marcos), así que el primer contacto es con él de todos modos. Así la web
   no guarda datos personales ni toca el backend.
5. **Los datos de la escuela viven en un solo fichero** (`app/(web)/_parts/school.ts`):
   WhatsApp, dirección, precios, días y horas, edades, tamaño de los grupos, bio y respuestas.
   Lo que falte se pinta como marcador visible (`[Precio]`), nunca como un dato que parezca
   real. Los textos sobre la app sólo dicen lo que hace hoy: cancelar hasta 24 horas antes, que
   Marcos da de alta y que la app no cobra.
6. **El horario no se publica en vivo.** Necesitaría un endpoint público nuevo. Los días y las
   horas de cada grupo son texto de `school.ts`.
7. **Niños: "escríbele a Marcos".** Una cuenta es una persona hasta la Fase 26 (familias); la web
   no promete que un adulto reserve por su hijo.
8. **Claro y oscuro, sólo en la web.** Sin elección, sigue al sistema (`prefers-color-scheme`).
   El botón de la cabecera alterna el tema y lo guarda en la cookie `theme`, no httpOnly y sin
   datos personales. El servidor la lee al pintar y pone `data-theme` en el contenedor de la
   web, así que no hay parpadeo ni hace falta un script en línea antes de hidratar, que la CSP
   bloquearía. Los tokens oscuros se definen junto a los claros en `globals.css`, para que la
   app pueda adoptarlos más adelante sin rehacerlos.
9. **Renderizado por petición**, como el resto: la cookie del tema ya lo exige, y el HTML sale
   completo del servidor, que es lo que necesitan los buscadores. Nada de estático por ahora.
10. **SEO.** `metadata` propia de la web (título, descripción, Open Graph con una imagen
    estática), `robots.ts` y `sitemap.ts` con `/`, y JSON-LD `SportsActivityLocation` con el
    nonce de la petición. Las pantallas de la app quedan fuera del sitemap.
11. **Movimiento**, sólo uno: la entrada de la portada al cargar, con `@keyframes` y clases.
    No se usan librerías que escriben `style` al renderizar en el servidor, porque la CSP lo
    bloquea sin avisar. La regla global de `prefers-reduced-motion` ya lo apaga.
12. **Imágenes**, con `next/image` desde `public/`. Hasta que lleguen las fotos reales, un
    marcador con la descripción de la foto que va en ese sitio. Sin mapa incrustado (cookies de
    terceros y `frame-src`): dirección y un enlace "Cómo llegar".

## Lo que no entra

- El vídeo de cabecera: cuando haya grabaciones. Irá servido desde este origen (no YouTube),
  silenciado, en bucle, con imagen de portada y botón de pausa.
- El horario con plazas en vivo, la reserva de la prueba en la web y las opiniones.
- El modo oscuro de la app y su manifest e icono de instalación (Fase 30).

## Criterios de aceptación

1. Sin sesión, `/` muestra la web y el botón "Pedir clase de prueba" lleva a `wa.me` con el
   número de `school.ts` (E2E).
2. Con sesión de alumno, "Mi cuenta" lleva a `/home`; con la del profesor, a `/teacher` (E2E).
3. Sin cookie, la web sigue el tema del sistema; el botón lo cambia y el cambio sobrevive a una
   recarga (E2E, con `colorScheme` de Playwright).
4. Ni la web ni las pantallas de acceso registran violaciones de CSP en la consola (E2E).
5. axe sin fallos `serious` ni `critical` en la web, en claro y en oscuro, en móvil y en
   escritorio (E2E con `@axe-core/playwright`).
6. `/robots.txt` y `/sitemap.xml` responden 200, y la portada lleva el JSON-LD (E2E).
7. Ningún texto, título ni `aria-label` de la app dice "MAS" (búsqueda pegada en el PR).
8. Lighthouse de la build de producción en la web: 90 o más en rendimiento, accesibilidad,
   buenas prácticas y SEO (salida pegada en el PR).
9. Los E2E anteriores siguen en verde, con la marca nueva.

## Riesgos

- **Marcadores en producción.** Un `[Precio]` publicado parece un descuido. Antes de desplegar
  (Fase 17), `school.ts` no puede tener ninguno: se comprueba ahí.
- **El número de WhatsApp es público.** Es lo que se quiere, pero atraerá spam; Marcos decide
  si usa su número o uno de la escuela.
- **La cookie del tema no es esencial**, pero tampoco rastrea: es una preferencia que pide el
  propio visitante, así que no necesita aviso de cookies.

## Decisiones tomadas al implementar

- **El JSON-LD va sin nonce.** El navegador no ejecuta un bloque `application/ld+json`, así que
  la CSP no tiene nada que bloquear, y el nonce en un `<script>` hidratado puede dar avisos de
  hidratación. Corrige la decisión 10.
- **La entrada de la portada sólo se mueve, no aparece desde transparente.** Un texto con
  opacidad 0 no cuenta como pintado: el título tardaba en contar como el elemento más grande de
  la página hasta que acababa la animación.
- **En el móvil, el botón de la prueba vive sólo en la barra de abajo**, que no se va al hacer
  scroll. Los de la portada y del final se ven a partir de 1024 px: si no, la primera pantalla
  del móvil lo enseñaba dos veces.
- **El logo de la app lleva al inicio de cada rol**, no a `/`: `/` es ahora la web.
- **Sin número de WhatsApp en `school.ts`, el botón lleva a la sección de contacto** (`#donde`);
  con número, a `wa.me`. Lo cubren el test unitario y el E2E.
- **`robots.txt` y `sitemap.xml` se generan al compilar**, así que `SITE_URL` tiene que estar en
  el entorno de la build de producción, no sólo en el de ejecución.
- **Lighthouse en el móvil, con la máquina libre** (12 de octubre, cinco pasadas sobre la build de
  producción): rendimiento entre 86 y 93, mediana 90; accesibilidad 100, buenas prácticas 96, SEO
  100. En escritorio: 99, 100, 96 y 100. Con las fotos, el LCP del móvil es la de la portada, y se
  precarga desde el `<head>` (`preload` de `react-dom`): bajó el retraso de carga de 480 a 110 ms.
  Lo que queda (3,3 s de LCP) es que Lighthouse no da la imagen por pintada hasta ejecutar el
  JavaScript de React y Next con la CPU del móvil simulado cuatro veces más lenta; reducirlo es de
  la Fase 30. El 96 de buenas prácticas es el 403 y el 401 que deja en la consola el intento de
  recuperar la sesión en cada visita anónima.
- **Faltan el aviso legal y la política de privacidad.** La web no recoge datos personales, pero
  la app sí; tienen que existir antes de desplegar (Fase 17).
- **Datos de prueba mientras se enseña la web.** Por decisión de Daniel, el teléfono, el correo,
  los horarios y el enfoque de Marcos son inventados y van marcados `DEMO` en `school.ts`; el
  resto (zona, precios, nombre, respuestas) es real. Corrige la decisión 5 hasta desplegar: el
  riesgo de los marcadores se amplía a ellos. El WhatsApp sigue vacío, para que el botón no abra
  un chat con quien tenga un número inventado, y el correo usa un dominio reservado (`.example`).
- **Las fotos son de Marcos jugando**, no de una clase: la de la portada y un recorte de la del
  Campeonato de Fuerteventura 2026 sin el cartel ni los patrocinadores. Van ya redimensionadas en
  webp en `public/web`.
- **`<img>` en vez de `next/image`.** `next/image` escribe un atributo `style`, que la CSP bloquea
  y el E2E de la CSP detecta. Corrige la decisión 12.
