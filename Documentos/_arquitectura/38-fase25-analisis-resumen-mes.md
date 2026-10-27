# Fase 25 — Análisis: resumen del mes

Criterio de salida (`12-metodologia-trabajo.md`): clases dadas, ocupación, asistencia y alumnos
activos.

`01` §18 no fija ninguna regla para el resumen. Sólo dice que las reservas pasadas de una cuenta
borrada quedan como «Alumno eliminado», para que los resúmenes cuadren.

## Punto de partida

- **Todo lo que hace falta ya se guarda**:
  - en las clases, su hora, tipo, plazas, nivel y si se cancelaron;
  - en las reservas, su estado y su asistencia (`PENDING`, `ATTENDED` o `NO_SHOW`).
- **No hay nada que sume.** Hoy Marcos sólo ve su agenda semana a semana.
- **Un mes es una idea local**: el de Marcos, en su zona, como «las clases del día 3»
  (`GetLesson`).

## Decisiones propuestas

### 1. Qué cuenta cada número

- **Clases dadas**: las del mes que ya han empezado y no se cancelaron, separadas en individuales
  y de grupo.
- **Ocupación**: las plazas ocupadas entre las plazas ofrecidas, en esas mismas clases. Una
  clase individual con su alumno cuenta 1 de 1.
- **Asistencia**: los que vinieron entre aquellos a los que se pasó lista. Las plazas sin pasar
  lista no cuentan ni a favor ni en contra, y el resumen dice cuántas quedan («3 plazas sin pasar
  lista»).
- **Clases canceladas**: cuántas, y cuántas de ellas en el último momento (menos de 24 horas). La
  lluvia se ve aquí.
- **Las reservas de un alumno que borró su cuenta siguen contando**, como dice `01` §18.

### 2. Qué es un alumno activo — **decidido: A**

- **A. El que ha tenido al menos una clase en el mes**: una plaza en una clase
  dada, haya venido o no. Es lo que dice cuánta gente se mueve de verdad cada mes.
- **B. El que Marcos gestiona a final de mes**, venga o no. Este número ya lo enseña la lista de
  alumnos.

### 3. Qué enseña la pantalla — **decidido: A**

- **A. Los números del mes, con el mes anterior al lado, y el mismo desglose por nivel.** Así se ve si un nivel se llena y otro no, que es la pregunta que viene después de «¿cómo
  va el mes?».
- **B. Sólo los números del mes**, sin comparar ni desglosar.

En los dos casos:
- Una pantalla nueva, «Resumen», en el menú de Marcos, con flechas para cambiar de mes.
- Por defecto se ve el mes en curso, contado hasta hoy.
- Los meses futuros no se pueden elegir.

### 4. Se calcula al leer

- **No hay tabla nueva.** El resumen se cuenta cada vez que se pide, porque con un profesor y una
  pista un mes son unas decenas de clases. Corregir la asistencia cambia el resumen al momento.
- **Lo calcula `calendar`**, que ya junta clases y reservas y sólo puede leer.

### 5. Sin datos personales

- El resumen sólo da números, nunca nombres. Los logs no tienen nada nuevo que callar.

## Contrato

Todo va primero a `openapi.yaml`:
- `GET /teacher/summary?month=2026-10`, sólo para el profesor. Si falta `month`, responde el mes en
  curso.
- La respuesta lleva:
  - los números del mes;
  - con la opción A de la decisión 3, los del mes anterior y una lista por nivel, en la que
    «abiertas a todos» es una fila más.
- Errores:
  - un mes futuro o mal escrito da 400 `VALIDATION_ERROR`;
  - el alumno y el admin reciben 403.
- `npm run api:types`, la fila nueva en `AccessMatrixTest` y `11-contrato-api.md`.

## Fuera, y por qué

- **Ingresos**: llegan con los bonos (Fase 27) y las mensualidades (Fase 28), que es cuando la app
  sabe algo de dinero.
- **Descargar en Excel o PDF**: nadie lo ha pedido. Se añade si Marcos lo necesita para su gestor.
- **Gráficas por semanas o por años**: con la comparación con el mes anterior basta para empezar.
- **Un resumen para el alumno**: su historial ya está en «Mis reservas».

## Tests

Con `Clock` fijo y fechas literales, cada número con su caso límite:
- **Clases dadas**:
  - una cancelada no cuenta;
  - una que aún no ha empezado no cuenta;
  - una del día 1 a las 00:30 en Madrid es de ese mes aunque en UTC sea del anterior.
- **Ocupación**: las plazas canceladas no ocupan.
- **Asistencia**: las plazas sin pasar lista quedan fuera del porcentaje y se cuentan aparte.
- **Alumnos activos**: un alumno con dos clases cuenta una vez, y uno que borró su cuenta sigue
  contando.
- **Desglose por nivel** (si es la opción A): las filas suman el total.
- **Mes futuro** da 400, y el alumno recibe 403.
- **E2E**: Marcos abre «Resumen» y ve la clase de la prueba en el mes.

**Mutaciones**, cada una se ve fallar y se deshace:
- contar las canceladas;
- meter las plazas sin pasar lista en la asistencia;
- cortar el mes en UTC.

## Criterios de aceptación

- Existe un test por cada número y por cada caso límite de arriba.
- Las mutaciones se han visto fallar.
- `mvn verify` y el CI en verde, sin tests saltados, y el E2E nuevo pasa.

## Orden de trabajo

1. Contrato y matriz de acceso.
2. Los puertos de lectura que falten en `lesson` y `booking`.
3. El cálculo en `calendar`, con sus tests.
4. Frontend: la pantalla «Resumen».
5. E2E y documentación: `02`, `11-contrato-api.md` y `CLAUDE.md` si aparece alguna trampa.

## Decisiones tomadas al implementar

- **Daniel eligió A en las dos decisiones.** Preguntó además por los pagos vencidos: llegan con la
  Fase 28, donde quedó apuntado que el alumno sigue activo y Marcos ve el aviso.
- **Un mes futuro o mal escrito da `DATE_RANGE_INVALID`**, el código de los rangos de fechas, y no
  `VALIDATION_ERROR`.
- **El backend da los recuentos y el frontend los porcentajes**: así el contrato no tiene que
  decidir cómo se redondea, y un mes sin plazas no tiene que inventarse un 0 %.
- **Las cancelaciones también se cuentan hasta ahora** en el mes en curso, como las clases dadas: una
  clase cancelada de la semana que viene no es aún del resumen.
- **«Resumen» va en la barra lateral, y no en la barra de abajo del móvil**, que con seis pestañas
  ya no tiene sitio. En el móvil se llega desde «Hoy».
- **El E2E abre el resumen y cambia de mes, sin comprobar números**: contra el reloj real no se puede
  crear una clase ya dada. Los números se prueban con `Clock` fijo en `GetMonthSummaryServiceTest`, y
  el de la API mueve una clase al principio del mes.
- **Mutaciones**, todas vistas fallar y deshechas: contar las canceladas como dadas, contar las
  plazas sin pasar lista como asistidas, cortar el mes en UTC y contar las clases que aún no han
  empezado.
