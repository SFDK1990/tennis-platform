# Fase 29 — Análisis: progreso del alumno

Criterio de salida (`12-metodologia-trabajo.md`): objetivos por nivel que Marcos marca como
conseguidos.

La Fase 23 lo dejó apuntado (`36-fase23-analisis-niveles-ficha.md`, decisión 6): los «objetivos» de
la ficha son texto libre, lo que Marcos quiere trabajar con *este* alumno; los objetivos *por nivel*,
iguales para todos los alumnos del nivel y que se marcan uno a uno, son esta fase. `01` §18 dice
que el nivel lo decide Marcos: el progreso le ayuda a decidirlo, no lo cambia solo.

## Punto de partida

- **Los niveles son de Marcos** (`levels`, módulo `teacher`): nombre y orden, los cuatro iniciales
  los crea el bootstrap, y uno en uso no se borra (`ON DELETE RESTRICT`, `LEVEL_IN_USE`).
- **El alumno tiene un nivel o ninguno** (`teacher_students.level_id`, módulo `student`), y la ficha
  ya tiene `goals` y `private_notes` (texto libre, sólo para Marcos).
- **El alumno no ve hoy nada de su ficha** en la app, salvo su nivel en `GET /me`. Objetivos y notas
  sólo salen en la exportación de sus datos.
- **El tablón de avisos de la Fase 24** ya lleva mensajes de Marcos al alumno.
- **Las fichas y las listas** ya existen: un apartado nuevo en la ficha no pide pantalla nueva.

## Decisiones propuestas

### 1. Si el alumno ve su progreso

- **A (te la recomiendo). Sí.** En su perfil, «Mi progreso»: los objetivos de su nivel, los
  conseguidos con su fecha y una barra «4 de 7». Es lo que hace que el alumno quiera volver a la
  app, y no enseña nada que Marcos no haya escrito para todos los del nivel.
- **B. Sólo Marcos.** El progreso es una herramienta suya para decidir subidas de nivel.

Las notas privadas siguen sin verse, y los objetivos de texto libre de la Fase 23 también.

### 2. Qué estados tiene un objetivo para un alumno

- **A (te la recomiendo). Dos: pendiente o conseguido**, con el día. Un clic en la ficha.
- **B. Tres: pendiente, trabajándolo y conseguido.** Da más detalle, pero Marcos tiene que tocar
  cada objetivo dos veces y «trabajándolo» ya lo dicen los objetivos de texto libre.

### 3. Qué pasa con lo conseguido cuando el alumno cambia de nivel

- **A (te la recomiendo). Se queda.** «Mi progreso» enseña el nivel actual y, debajo, los anteriores
  con lo que consiguió en cada uno. Si Marcos le baja de nivel, encuentra marcado lo que ya tenía.
- **B. Se empieza de cero** en el nivel nuevo, y lo anterior sólo lo ve Marcos en la ficha.

### 4. Los objetivos con que se empieza

- **A (te la recomiendo). Ninguno: los escribe Marcos.** Es su método, y la pantalla de niveles,
  vacía, le guía a escribirlos (como las pantallas vacías de la Fase 15.5).
- **B. Una lista de partida** de cinco o seis por nivel (por ejemplo, en iniciación «peloteo de diez
  golpes desde el fondo»), que el bootstrap crea y Marcos cambia. Se estrena antes, pero son objetivos
  que no ha escrito él.

### 5. Si el alumno se entera de que ha conseguido uno

- **A (te la recomiendo). Un aviso en la app**, en el tablón de la Fase 24: «Marcos ha marcado
  "saque con segundo saque liftado" como conseguido». Sin correo.
- **B. Nada**: lo ve cuando entra en «Mi progreso».
- **C. Aviso y correo.** `01` §18 deja los correos a las reservas y los mensajes de Marcos; un correo
  por objetivo sería ruido.

## Reglas (se desarrollan de §18, no se preguntan)

**Los objetivos del nivel:**
- Cada nivel tiene una lista ordenada de objetivos: un título de hasta 120 caracteres, único dentro
  del nivel, y el orden en que Marcos los quiere. Se crean, renombran y reordenan desde la pantalla
  de niveles.
- **Un objetivo que algún alumno ya consiguió no se borra, se retira**: deja de salir como pendiente
  para nadie, y quien lo consiguió lo sigue viendo conseguido. Uno que nadie consiguió se borra.
- **Un nivel con objetivos conseguidos está en uso** (409 `LEVEL_IN_USE`), como uno con alumnos o
  clases. Uno con objetivos que nadie consiguió se borra con ellos.
- Renombrar un objetivo cambia lo que ven todos, también quien ya lo consiguió.

**Marcar:**
- Sólo el profesor que gestiona al alumno, desde la ficha. El día es hoy, en la zona de Marcos, y se
  puede cambiar a uno pasado (no futuro).
- Puede marcar un objetivo de cualquier nivel, no sólo del actual: un alumno que ya domina algo del
  nivel siguiente lo tiene apuntado cuando suba.
- **Desmarcar** borra la marca; un objetivo marcado por error no deja rastro.
- Marcar dos veces el mismo es idempotente: el `PUT` deja el día que se le pase.
- **Al completar todos los objetivos de su nivel**, la ficha lo dice («Ha conseguido todos los
  objetivos de Intermedio») y la lista de alumnos lo señala. Subirle sigue siendo cosa de Marcos.

**Lo que ve el alumno** (con la decisión 1 en A):
- Sin nivel: «Marcos aún no te ha puesto nivel», sin lista.
- Un nivel sin objetivos: «Marcos aún no ha puesto objetivos para tu nivel».
- Los retirados que no consiguió no aparecen.

**Datos personales:** lo conseguido es un dato del alumno. Sale en la exportación, **se borra al
borrar la cuenta** (`DeleteMyAccountTest`), y dejar de gestionar al alumno lo conserva: si Marcos
lo vuelve a gestionar, lo encuentra. Nunca sale en un log.

## Contrato

Todo va primero a `openapi.yaml`:
- **Objetivos de un nivel**:
  - `GET /teacher/levels` gana `objectives` (id, título, orden, retirado);
  - `POST /teacher/levels/{id}/objectives`;
  - `PATCH /teacher/objectives/{id}` para título y orden;
  - `DELETE /teacher/objectives/{id}`: 204 si se borra, y el objetivo pasa a retirado si alguien lo
    había conseguido (la respuesta lo dice con 200 y el objetivo).
- **Marcar**: `PUT /teacher/students/{userId}/objectives/{objectiveId}` con `achievedOn` (opcional)
  y `DELETE` para desmarcar. Un alumno que no gestiona da 404 `STUDENT_NOT_FOUND`, como la ficha.
- **La ficha**: `StudentDetail` gana `progress`, por nivel, con cada objetivo y su día si lo tiene.
  La lista de alumnos gana `levelCompleted`.
- **El alumno**: `GET /me/progress`, con la misma forma.
- **Códigos nuevos**: `OBJECTIVE_NOT_FOUND` (404), `OBJECTIVE_ALREADY_EXISTS` (409, título repetido
  en el nivel) y `ACHIEVED_ON_IN_FUTURE` (422).
- `npm run api:types`, las filas nuevas en `AccessMatrixTest` y `11-contrato-api.md`.

**Tablas nuevas**: `level_objectives` (nivel, título, orden, retirado) en `teacher`, junto a los
niveles, y `student_objectives` (alumno, objetivo, día; única por alumno y objetivo) en `student`,
junto a la ficha. `student_objectives` apunta al objetivo con `ON DELETE RESTRICT`: lo que impide
borrar un objetivo conseguido es la base, no sólo la aplicación. Las dos entran en el `TRUNCATE`.
El aviso de la decisión 5 lo envía `student` por una spi que implementa `notification`, como los
mensajes.

## Fuera, y por qué

- **Subir de nivel solo al completar los objetivos**: `01` §18 deja el nivel a Marcos.
- **Que el alumno marque o proponga objetivos**: mismo motivo.
- **Objetivos de un alumno concreto**: ya son el texto libre de la Fase 23.
- **Notas, vídeos o puntuación por objetivo**: no se han pedido; con dos estados basta para empezar.
- **Objetivos conseguidos en el resumen del mes**: se añade si Marcos lo echa en falta.

## Tests

- **Objetivos del nivel**:
  - crear, renombrar y reordenar; título repetido en el nivel, 409;
  - borrar uno sin conseguir lo borra; uno conseguido lo retira, y la base impide borrarlo;
  - un nivel con objetivos conseguidos da `LEVEL_IN_USE`; sin conseguir, se borra con ellos.
- **Marcar**:
  - sólo el profesor que lo gestiona; otro alumno o un alumno sin gestionar, 404;
  - el día por defecto es hoy en la zona de Marcos, con `Clock` fijo; un día futuro, 422;
  - marcar dos veces deja una fila; desmarcar la borra;
  - un objetivo de otro nivel se puede marcar.
- **Lo que ve el alumno**: su nivel y los anteriores; sin nivel; nivel sin objetivos; los retirados
  que no consiguió no salen; no ve los de otro alumno.
- **Completar el nivel** pone `levelCompleted` en la lista y la ficha; un objetivo nuevo en el nivel
  lo quita.
- **El aviso** llega al tablón del alumno al marcar, y no al desmarcar.
- **Borrar la cuenta** borra lo conseguido; la exportación lo incluye; dejar de gestionar lo
  conserva.
- **E2E**: Marcos escribe dos objetivos en un nivel, marca uno en la ficha de un alumno; el alumno ve
  «1 de 2» y el aviso.

**Mutaciones**, cada una se ve fallar y se deshace:
- dejar marcar a un profesor que no gestiona al alumno;
- borrar un objetivo conseguido en lugar de retirarlo;
- no borrar lo conseguido al borrar la cuenta;
- enseñar al alumno los retirados que no consiguió.

## Criterios de aceptación

- Existe un test por cada regla de arriba.
- Las mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Objetivos del nivel: tabla, CRUD, retirar y la pantalla de niveles.
3. Marcar y desmarcar desde la ficha; `levelCompleted` en la lista.
4. «Mi progreso» del alumno y el aviso.
5. Borrado, exportación y `DeleteMyAccountTest`.
6. E2E y documentación: `03`, `10-diagrama-er.md`, `11-contrato-api.md` y `CLAUDE.md`.
