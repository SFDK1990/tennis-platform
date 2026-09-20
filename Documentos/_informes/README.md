# Informes de entrega

Lo que se hizo, con la evidencia pegada. Un fichero por entrega, con la fecha en el nombre.

La metodología de trabajo pide que ninguna fase se dé por terminada sin la salida real de los
comandos, y no solo un `BUILD SUCCESS` afirmado de memoria. Esa evidencia tiene que vivir en
algún sitio: los informes estaban sueltos en la raíz del repositorio y sin versionar, donde
sobrevivían exactamente hasta la primera limpieza del directorio.

## En qué se diferencian de `_arquitectura/`

`_arquitectura/` responde **cómo es el sistema y por qué**, y es vinculante: si el código no
coincide con esos documentos, lo que está mal es uno de los dos y hay que arreglarlo.

Esta carpeta responde **qué pasó en una entrega concreta**, y no es vinculante: es un registro
fechado. Un informe no se actualiza cuando la realidad cambia, porque era cierto el día que se
escribió. Cuando una decisión de un informe pasa a ser permanente, su sitio es `_arquitectura/`
o `CLAUDE.md`, no una edición retroactiva de aquí.

Por eso mismo **no hay que leer estos informes para trabajar en el proyecto**. Sirven para
reconstruir por qué se tomó una decisión, o para comprobar qué se verificó de verdad y qué se
dio por bueno sin mirar.

## Qué no está aquí

El fichero de traspaso entre sesiones (`handoff-siguiente-sesion.md`, en la raíz) está en el
`.gitignore` a propósito: se reescribe entero cada vez y describe un estado que es falso un día
después. Un informe es lo contrario — se escribe una vez y no se toca.
