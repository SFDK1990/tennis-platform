# Fase 26 — Análisis: familias

Criterio de salida (`12-metodologia-trabajo.md`): un adulto gestiona los perfiles de sus hijos y
reserva por ellos.

La regla ya está decidida (`01` §18): los hijos son perfiles dentro de la cuenta de un adulto, que
reserva por ellos. El niño no tiene email ni contraseña.

## Punto de partida

- **Todo lo del alumno cuelga de su cuenta** (`users.id`): la relación con Marcos, el nivel y la
  ficha, las reservas, la lista de espera, los mensajes, la foto y el resumen. Hay unas veinte
  claves que apuntan ahí.
- **Una cuenta tiene email y contraseña obligatorios**, y para reservar hace falta el email
  verificado.
- **Marcos añade a un alumno buscándolo por su email exacto**, y un niño no tiene email.
- **Los correos** (reserva, cancelación, cambio de pista, mensajes) van al email del alumno.

## Decisiones propuestas

### 1. Un hijo es un alumno sin acceso

- **El hijo es una cuenta de alumno sin email ni contraseña**, unida al adulto que la creó. Por eso
  no puede entrar en la app.
- Así todo lo que ya existe le sirve tal cual: Marcos lo gestiona, le pone nivel y ficha, reserva
  sus plazas, le pasa lista, le escribe y lo cuenta en el resumen. No hay que duplicar las
  reservas ni los avisos.
- Otra opción era una tabla aparte de hijos, pero obligaría a rehacer las reservas, la lista de
  espera y todo lo que hoy apunta a una cuenta.

### 2. El adulto también puede ser alumno — **decisión tuya**

- **A (te la recomiendo). El adulto es alumno como hasta ahora y, además, puede tener hijos.** Una
  madre que juega y apunta a sus dos hijos lo hace todo con una cuenta.
- **B. Una cuenta de familia sólo gestiona a los hijos**, y si el adulto quiere jugar se hace otra
  cuenta con otro email.

### 3. Qué hace el adulto

- **En «Perfil» añade a sus hijos**, con nombre y fecha de nacimiento. Los edita o los quita.
- **Arriba de «Clases», «Mis reservas» e «Inicio» elige para quién** («Yo», «Lucía», «Pablo»).
  Reservar, cancelar y apuntarse a la lista de espera se hacen para esa persona.
- Las reglas son las de siempre, cada una para quien va a la clase: su nivel, que Marcos lo
  gestione, no tener dos clases a la vez y la ventana de 24 horas.
- **El email que cuenta es el del adulto.** Mientras el adulto no lo verifique, nadie de la familia
  reserva.

### 4. Cómo llega el hijo a Marcos

- Marcos busca el email del adulto, como ahora, y ve también a sus hijos. Añade a los que vengan a
  clase, uno a uno.
- En sus listas el hijo aparece con su nombre y con «hijo de …», para saber a quién llamar.

### 5. Los correos van al adulto

- Las confirmaciones, cancelaciones, cambios de pista y mensajes de un hijo llegan al email del
  adulto, diciendo para quién son («Reserva de Lucía confirmada»).

### 6. La foto del hijo — **decisión tuya**

- **A (te la recomiendo). El adulto la sube desde el perfil del hijo**, igual que la suya. Marcos la
  ve como la de cualquier alumno.
- **B. Los menores no tienen foto.** Es lo más prudente con datos de niños, pero Marcos pierde la
  cara en la lista.

### 7. Datos personales

- **Del hijo se guarda sólo su nombre y su fecha de nacimiento**, y la foto si se elige A en la
  decisión 6. No hay DNI, teléfono ni dirección: son los del adulto.
- **Borrar la cuenta del adulto borra también a sus hijos**, de la misma forma que hoy se borra un
  alumno: las reservas futuras se cancelan y las pasadas quedan como «Alumno eliminado».
- **Quitar a un hijo** es borrar su perfil de la misma forma.
- **La exportación del adulto incluye la de sus hijos.**

## Contrato

Todo va primero a `openapi.yaml`:
- `GET /me/children`, `POST /me/children`, `PATCH /me/children/{id}` y `DELETE /me/children/{id}`.
- Reservar, cancelar, la lista de espera, el calendario y «Mis reservas» aceptan `studentUserId`, que
  tiene que ser el adulto o uno de sus hijos. Sin él, es el adulto, como hasta ahora.
- `GET /teacher/students/lookup` devuelve también los hijos de esa cuenta.
- Las fichas y listas del profesor dicen quién es el adulto de un hijo.
- Errores:
  - un `studentUserId` que no es de la familia da 404, como una reserva ajena;
  - un hijo sin nombre da 400.
- `npm run api:types`, las filas nuevas en `AccessMatrixTest` y `11-contrato-api.md`.

## Fuera, y por qué

- **Que un hijo mayor pase a tener su propia cuenta**: hará falta el día que ocurra, y se puede hacer
  dándole un email.
- **Dos adultos para el mismo hijo**: con uno basta para empezar.
- **Descuentos para hermanos**: llegan, si llegan, con los bonos y las mensualidades.

## Tests

- **Crear, editar y quitar** un hijo; sin nombre, 400.
- **Un hijo no puede entrar**: no hay email ni contraseña con los que hacerlo.
- **Reservar por un hijo**:
  - se aplica su nivel y no el del adulto;
  - hace falta que Marcos lo gestione;
  - el hijo puede tener una clase a la misma hora que su madre;
  - y el correo llega al adulto, con el nombre del hijo.
- **Acceso**: el `studentUserId` de un hijo de otra familia da 404 en reservar, cancelar y leer.
- **Marcos** encuentra a los hijos por el email del adulto y los gestiona.
- **Borrar al adulto** borra a los hijos, cancela sus reservas futuras y deja las pasadas.
- **E2E**: un adulto añade a su hijo, Marcos lo gestiona y el adulto le reserva una clase.

**Mutaciones**, cada una se ve fallar y se deshace:
- no comprobar que el hijo sea de la familia;
- aplicar el nivel del adulto;
- no borrar a los hijos con el adulto.

## Criterios de aceptación

- Existe un test por cada regla de arriba.
- Las mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Cuentas sin acceso unidas a un adulto: migración y alta de hijos.
3. Actuar por un hijo en reservas, lista de espera, calendario y correos.
4. Lo que ve Marcos: búsqueda, listas y fichas.
5. Borrado y exportación.
6. Frontend: los hijos en «Perfil» y el selector «Para quién».
7. E2E y documentación.

## Decisiones tomadas al implementar
