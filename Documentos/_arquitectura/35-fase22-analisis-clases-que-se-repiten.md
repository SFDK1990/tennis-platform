# Fase 22 — Análisis: clases que se repiten

Criterio de salida (`12-metodologia-trabajo.md`): la misma clase durante varias semanas de una vez.

La regla ya está decidida (`01` §18): antes de crearlas se muestran las fechas que chocan, con
otra clase o fuera del horario, y Marcos decide si salta esas o cambia algo.

## Punto de partida

- **Una clase se crea de una en una** (`POST /teacher/lessons`, `ScheduleLessonService`). Se
  comprueba, en este orden:
  1. que no ha empezado;
  2. que cae dentro del horario (`QueryAvailability.covers`), salvo que se pida explícitamente
     con `overrideAvailability`;
  3. que no choca con otra clase. Lo comprueba la aplicación, para dar un 409 útil, y también la
     restricción `EXCLUDE` de la tabla.
- **La clase no cruza medianoche** en la zona del profesor, y su duración se mide en instantes.
- **La disponibilidad es hora de pared**: «los martes de 18:00 a 19:00» es a las 18:00 de Madrid
  también la semana del cambio de hora.
- Hoy nada agrupa clases: cada una es independiente.

## Decisiones propuestas

### 1. Cada semana, el mismo día y a la misma hora de pared

- El formulario de crear clase gana **«Repetir cada semana»** y **«Durante N semanas»**, contando
  la primera.
- **La misma hora de pared en la zona del profesor**, no los mismos instantes. Así una clase de
  los martes a las 18:00 sigue a las 18:00 después del cambio de hora, igual que el horario.
- Tipo, duración, capacidad y notas, iguales en todas.
- Sólo semanal. Cada dos semanas o «el primer lunes del mes» no los ha pedido nadie.

### 2. Primero se ven las fechas, luego se crean

Son dos pasos:
1. **`POST /teacher/lessons/series/preview`** devuelve cada fecha con su estado. No guarda nada.
   Los estados son:
   - `OK`;
   - `OUTSIDE_AVAILABILITY`: fuera del horario;
   - `OVERLAP`: choca con otra clase;
   - `IN_THE_PAST`: sólo puede pasarle a la primera.
2. **`POST /teacher/lessons/series`** crea las fechas que Marcos ha dejado marcadas.

En la pantalla, las fechas con problemas salen desmarcadas y con el motivo:
- **Fuera de horario** se puede marcar igualmente, como hoy con una clase suelta.
- **Las que chocan con otra clase no se pueden crear.** La otra clase ocupa la pista, y la base
  lo impediría de todas formas.

### 3. Todo o nada

- Las clases elegidas se crean en **una transacción**: o todas o ninguna.
- Si entre la vista previa y la creación otra petición ocupa una de las fechas, la respuesta es
  409 y la pantalla vuelve a pedir la vista previa. No queda una serie a medias que Marcos tenga
  que revisar a mano.

### 4. Hasta 16 semanas — **decisión tuya**

Te propongo un tope de **16 semanas**, un cuatrimestre. Sirve para crear el curso entero de una
vez, y evita crear un año de clases por un error en el número. Si prefieres otro tope, se cambia
aquí.

### 5. ¿Quedan unidas? — **decisión tuya**

- **A (te la recomiendo). Cada clase lleva el id de su serie.** En el detalle se ve «Se repite
  cada martes, 3 de 12», y aparece **«Cancelar esta y las siguientes»**, para cuando el curso
  termina antes o cambia de hora. La Fase 28 (mensualidades por grupo fijo) podrá apoyarse en
  ese vínculo.
- **B. Clases sueltas, sin vínculo.** Es más simple. Pero para cancelar el resto del curso hay que
  ir clase por clase, y la Fase 28 tendrá que inventar el vínculo después.

Con A, editar una clase de la serie sólo cambia esa clase: editar «todas las siguientes» no lo
ha pedido nadie.

## Contrato

Todo va primero a `openapi.yaml`:
- `POST /teacher/lessons/series/preview`: cuerpo `{ type, startsAt, endsAt, capacity, notes, weeks }`.
  Responde `{ occurrences: [{ startsAt, endsAt, status }] }`.
- `POST /teacher/lessons/series`: el mismo cuerpo, más `{ startsAt[] }` con las fechas elegidas
  y `overrideAvailability`. Responde 201 con las clases creadas.
- Con la opción A:
  - `seriesId` y `seriesPosition` en la vista de la clase;
  - `POST /teacher/lessons/{id}/cancel-following`, con el mismo motivo opcional que cancelar una
    clase. Avisa a los alumnos de cada una, como ya hace la Fase 20.
- Errores:
  - una fecha elegida que ya choca da 409 `LESSON_OVERLAP`;
  - más de 16 semanas, o una fecha que no es de la serie, da 400 `VALIDATION_ERROR`.
- `npm run api:types`, las filas nuevas en `AccessMatrixTest` y `11-contrato-api.md`.

## Fuera, y por qué

- **Otras repeticiones** (cada dos semanas, mensual): ver la decisión 1.
- **Editar todas las de la serie a la vez**: no se ha pedido. Cada clase se edita sola.
- **Reservar la serie entera de una vez** para un alumno: es la Fase 28 (mensualidades).
- **Aviso a los alumnos de clases nuevas**: hoy tampoco existe con una clase suelta, y llega con
  los mensajes (Fase 24).

## Tests

- **La hora de pared**: una serie que cruza el cambio de hora (fechas literales, como en
  `availability`) mantiene las 18:00 de Madrid en todas las clases.
- **La vista previa**, un test por estado: dentro del horario, fuera de él, choca con otra clase,
  y la primera fecha en el pasado.
- **Crear**:
  - sólo se crean las fechas elegidas;
  - una fecha que choca hace fallar todas, y no queda ninguna creada;
  - una fecha que no pertenece a la serie se rechaza;
  - el tope de semanas.
- **Con la opción A**:
  - el puesto en la serie;
  - «cancelar esta y las siguientes» deja las anteriores intactas y avisa a los alumnos de las
    canceladas;
  - las clases ya empezadas no se cancelan.
- **E2E**: Marcos crea una clase semanal de 4 semanas con una fecha que choca, la salta, y ve
  las otras 3 en la agenda.

**Mutaciones**, cada una se ve fallar y se deshace:
- sumar 7 días en instantes en vez de en hora de pared;
- crear fuera de una transacción;
- permitir marcar una fecha que choca.

## Criterios de aceptación

- Existe un test que demuestra que la serie mantiene la hora de pared al cruzar el cambio de hora.
- Existe un test por cada estado de la vista previa.
- Existe un test que demuestra que una fecha que choca no deja ninguna clase creada.
- Las tres mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Las fechas de la serie en hora de pared, con sus tests.
3. La vista previa.
4. Crear la serie, todo o nada.
5. Con la opción A: el id de serie, el puesto y «cancelar esta y las siguientes».
6. Frontend: «Repetir cada semana» y la lista de fechas.
7. E2E y documentación: `03`, `10-diagrama-er.md`, `11-contrato-api.md` y `CLAUDE.md`.

## Decisiones tomadas al implementar
