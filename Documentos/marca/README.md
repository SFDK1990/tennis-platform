# Marca — MAS Tennis Academy

Exploración de logotipo para la academia de Marcos Asencio Scattolo: diez conceptos en cinco
direcciones (monograma, trayectoria de la bola, geometría de pista, marca personal de lujo y alto
rendimiento), todos inspirados en la pista dura azul. Es una exploración, no una decisión: el
logotipo definitivo lo elige Daniel.

## Qué hay

```
conceptos/<concepto>/mark-{blue,black,white}.svg     símbolo en lienzo cuadrado (favicon, icono de app, avatar)
conceptos/<concepto>/lockup-{blue,black,white}.svg   símbolo con el nombre
generador/                                           script que produce todos los SVG
```

- **blue**: azul marino y azul pista, sobre fondo claro.
- **black**: una sola tinta negra.
- **white**: una sola tinta blanca, para fondos oscuros y fotografía.

Los SVG son **contornos rellenos**: sin texto vivo, sin trazos y sin máscaras. Lo que en la
versión azul parece blanco (las líneas de pista, las letras dentro de un sello) es un **hueco
real**, no una forma blanca, así que funciona igual sobre cualquier fondo y lo aceptan sin
retoques la imprenta, el bordado y el plotter de vinilo.

## Paleta

| Nombre | Hex | Papel |
|---|---|---|
| Baseline Navy | `#0B1E3F` | Estructura. El exterior oscuro de una pista dura. |
| Court Blue | `#1F5FB8` | Acento. La superficie de juego. |
| Blanco / Negro | `#FFFFFF` / `#000000` | Versiones de una tinta. |

## Los diez conceptos

| # | Dirección | Concepto | La idea en una frase | Punto débil principal |
|---|---|---|---|---|
| 01 | Monograma | Ball-Bar | La A cambia su barra por una bola. | La idea ya existe en otras marcas deportivas. |
| 02 | Monograma | Sello | Sello de club con el nombre completo en el anillo. | El texto del anillo no se lee por debajo de ~48 px. |
| 03 | Trayectoria | Flight S | La S es el vuelo de la bola y termina en ella. | El trazo fino inicial desaparece por debajo de 24 px. |
| 04 | Trayectoria | Tracking M | La M como traza de ojo de halcón, cada trazo más grueso; acaba en "M." | La idea del grosor creciente es sutil. |
| 05 | Pista | Court-Line Letters | Letras pintadas como líneas de pista, en la proporción de una pista de dobles. | Aspecto digital, menos premium. |
| 06 | Pista | Perspective Court A | La pista en perspectiva forma la A; la red es la barra. | A 16 px las líneas dobles se funden. |
| 07 | Lujo | The Net Line | Una línea fina a la altura de la barra de la A cruza el nombre como una red. | Las líneas finas se pierden en tamaño pequeño. |
| 08 | Lujo | Club Crest | Escudo de club clásico de una sola tinta. | El formato más común en deporte. |
| 09 | Rendimiento | Speed Cut | Cursiva condensada cortada por estelas de velocidad. | Las estelas son un recurso muy visto. |
| 10 | Rendimiento | Two Peaks | La M como dos cumbres, la segunda más alta. | Puede recordar a marcas de montaña. |

El simbolismo, los colores, la tipografía, las fortalezas y las debilidades de cada uno están
desarrollados en la presentación publicada junto a este trabajo.

**Propuesta**: 01 como logotipo principal de la academia, 07 como firma personal de Marcos
(ropa premium, sus redes) y 06 como símbolo pictórico si se quiere uno para muros y equipación.
Antes de decidir: búsqueda de marcas registradas y una prueba de bordado a 35 mm.

## Tamaños mínimos

- Símbolos 01, 04, 06, 09 y 10: legibles desde 16 px.
- 02 (sello completo) y 08 (escudo con bola): 48 px; por debajo, su símbolo simplificado.
- 07: la línea de red necesita unos 120 px de ancho.
- Bordado: 35 mm de ancho mínimo para cualquier logotipo con texto.

## Tipografías

Montserrat, Cormorant Garamond, Barlow Condensed y Outfit, todas con licencia SIL Open Font
License. Van convertidas a contornos dentro de los SVG, así que usar los logotipos no exige tener
las fuentes instaladas. Para textos que acompañen a la marca (web, documentos) sí hay que
cargarlas, y todas están en Google Fonts.

## Regenerar

```
cd Documentos/marca/generador
pip install -r requirements.txt
python build.py
```

La primera ejecución descarga las fuentes a `generador/.fonts/` (ignorada por git). Todo el
dibujo es geométrico y está en `build.py`: cambiar un grosor, un color o una proporción es
cambiar un número y volver a ejecutar, no retocar sesenta archivos a mano.
