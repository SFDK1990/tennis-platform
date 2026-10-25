# Fase 23 — Análisis: niveles y ficha del alumno

Criterio de salida (`12-metodologia-trabajo.md`): niveles de Marcos, clases por nivel, y una
ficha con objetivos, notas privadas e historial.

La regla ya está decidida (`01` §18): los niveles los define Marcos, empezando por iniciación,
intermedio, avanzado y competición. El alumno ve todas las clases, pero sólo reserva las de su
nivel o las abiertas a todos.

## Punto de partida

- **Ni las clases ni los alumnos tienen nivel.**
- **La ficha del alumno** (`GET /teacher/students/{userId}`) enseña sus datos personales: nombre,
  teléfono, DNI y dirección. Sólo la ve el profesor que lo gestiona.
- **La relación profesor–alumno** vive en `teacher_students`, del módulo `student`.
- **Reservar y la lista de espera** comprueban lo mismo, en el mismo orden (Fase 21). Una regla
  nueva entra en los dos sitios.
- **Borrar la cuenta vacía cada columna de texto** del alumno, y `DeleteMyAccountTest` falla si
  aparece una nueva que no se vacíe. **La exportación** (`GET /me/export`) devuelve todo lo que
  se guarda de él.

## Decisiones propuestas

### 1. Los niveles son de Marcos

- Una tabla `levels`: profesor, nombre y orden.
- **Nacen los cuatro de `01` §18**: iniciación, intermedio, avanzado y competición. La migración
  los crea para el profesor que ya existe, y el bootstrap para uno nuevo.
- Marcos los **renombra, añade, reordena y borra** desde una pantalla nueva, «Niveles», dentro de
  «Horario» o de su perfil.
- **Borrar un nivel que usa algún alumno o alguna clase** responde 409 `LEVEL_IN_USE`, con
  cuántos. Primero hay que cambiarlos de nivel. Borrarlo sin más dejaría alumnos que no saben qué
  pueden reservar.

### 2. El nivel del alumno está en la relación con Marcos

- `teacher_students.level_id`: el nivel lo pone el profesor, y si algún día hay dos profesores,
  cada uno tendrá sus niveles.
- Marcos lo elige en la ficha. El alumno lo ve en su perfil, sin poder cambiarlo.

### 3. La clase tiene un nivel, o es abierta a todos

- `lessons.level_id`, null para «abierta a todos». Se elige al crearla (también en una serie) y
  se puede cambiar al editarla.
- **Cambiar el nivel de una clase con reservas no cancela nada**: quien ya tiene plaza la
  conserva. Lo decidió Marcos al cambiarlo.
- La agenda y el calendario del alumno enseñan el nivel de cada clase.

### 4. Quién reserva qué

- Una clase abierta a todos la reserva cualquiera.
- Una clase con nivel la reserva sólo un alumno de ese nivel. **Sólo ese nivel, no los de
  debajo**, porque es lo que dice `01` §18.
- La comprobación va en reservar, en apuntarse a la lista de espera y en la promoción de la
  lista. Si no se cumple, la respuesta es 422 `LEVEL_NOT_ALLOWED`: releer no lo cambia.
- El alumno sigue viendo la clase, con «Para nivel avanzado» en lugar del botón.

### 5. Un alumno sin nivel — **decisión tuya**

Al desplegar esta fase, ningún alumno tendrá nivel hasta que Marcos se lo ponga.

- **A (te la recomiendo). Sin nivel, reserva cualquier clase.** Desplegar no cambia nada para
  nadie, y la regla empieza a contar con cada alumno en cuanto Marcos le pone nivel.
- **B. Sin nivel, sólo las abiertas a todos.** Es más estricto, pero el día del despliegue nadie
  podría reservar clases con nivel hasta que Marcos los repasara a todos.

### 6. La ficha del alumno

La ficha que ya existe gana tres apartados, todos sólo para Marcos:

- **Nivel**: el de la decisión 2.
- **Objetivos**: texto libre, por ejemplo «mejorar el revés». Los objetivos por nivel que se
  marcan como conseguidos son la Fase 29; esto es lo que Marcos quiere trabajar con este alumno.
- **Notas privadas**: texto libre que el alumno no ve en la app.
- **Historial**: sus reservas con Marcos, de la más reciente a la más antigua, con la asistencia
  y las canceladas.

Objetivos y notas van en `teacher_students` (`goals`, `private_notes`), hasta 2000 caracteres
cada uno.

### 7. Las notas privadas y los datos del alumno — **decisión tuya**

Objetivos y notas son datos personales del alumno.

- **Al borrar la cuenta se vacían**, como el resto (`DeleteMyAccountTest` lo exige).
- **Nunca salen en un log.**
- **En la exportación de sus datos** hay dos opciones:
  - **A (te la recomiendo). Salen.** El derecho de acceso del RGPD cubre lo que se guarda sobre
    una persona, también lo que escribe otro sobre ella. «Privadas» quiere decir que no se ven en
    la app, no que se le oculten si las pide.
  - **B. No salen.** Marcos escribe con más libertad, pero la exportación dejaría de ser
    completa.

## Contrato

Todo va primero a `openapi.yaml`:
- **Niveles**:
  - `GET /teacher/levels`;
  - `POST /teacher/levels`;
  - `PATCH /teacher/levels/{id}` para nombre y orden;
  - `DELETE /teacher/levels/{id}`, que da 409 `LEVEL_IN_USE` si alguien lo usa.
- **El alumno**: `PATCH /teacher/students/{userId}` con `levelId`, `goals` y `privateNotes`.
  `StudentDetail` los devuelve.
- **El historial**: `GET /bookings?studentUserId=` para el profesor, que ya filtra por clase. Un
  alumno que no gestiona da la lista vacía.
- **Las clases**: `levelId` al crear (también en series) y al editar. `Lesson` gana `levelId` y
  `levelName`, y la vista del calendario también.
- **El alumno ve su nivel**: `GET /me` gana `level`.
- `LEVEL_NOT_ALLOWED` (422) en reservar y en apuntarse a la lista de espera.
- `npm run api:types`, las filas nuevas en `AccessMatrixTest` y `11-contrato-api.md`.

## Fuera, y por qué

- **Que un alumno pida subir de nivel**: lo decide Marcos, como dice `01` §18.
- **Reservar clases de niveles inferiores**: ver la decisión 4.
- **Objetivos por nivel marcados como conseguidos**: es la Fase 29.
- **Filtrar el calendario por nivel**: el alumno ve todas las clases (`01` §18). El filtro se
  añade si Marcos lo echa en falta.

## Tests

- **Niveles**:
  - nacen los cuatro;
  - crear, renombrar y reordenar;
  - borrar uno en uso da 409, y uno sin usar se borra.
- **Reservar**, un test por fila:
  - clase abierta;
  - clase de su nivel;
  - clase de otro nivel, que da 422;
  - alumno sin nivel, según la decisión 5.
- **La misma regla** al apuntarse a la lista de espera y en la promoción: el primero de la lista
  con otro nivel se salta.
- **La ficha**:
  - sólo la ve y la cambia el profesor que lo gestiona;
  - el historial trae las reservas de ese alumno y de nadie más.
- **Borrar la cuenta** vacía objetivos y notas. La exportación los incluye o no, según la
  decisión 7.
- **E2E**:
  - Marcos pone nivel a un alumno y crea una clase de otro nivel;
  - el alumno la ve sin botón de reservar, y reserva una abierta.

**Mutaciones**, cada una se ve fallar y se deshace:
- quitar la comprobación de nivel al reservar;
- quitarla en la promoción de la lista;
- no vaciar las notas al borrar la cuenta.

## Criterios de aceptación

- Existe un test por cada fila de «quién reserva qué».
- Existe un test que demuestra que la promoción de la lista respeta el nivel.
- Existe un test que demuestra que borrar la cuenta vacía objetivos y notas.
- Las mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Niveles: tabla, los cuatro iniciales, CRUD y pantalla.
3. Nivel del alumno y de la clase.
4. La regla al reservar, al apuntarse y en la promoción.
5. La ficha: objetivos, notas e historial; borrado y exportación.
6. Frontend: niveles, ficha, nivel en la agenda y en el calendario del alumno.
7. E2E y documentación: `03`, `10-diagrama-er.md`, `11-contrato-api.md` y `CLAUDE.md`.

## Decisiones tomadas al implementar
