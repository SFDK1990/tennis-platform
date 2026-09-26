# Fase 14 — Análisis: cobertura y E2E

Criterio de salida (`12-metodologia-trabajo.md`): Playwright cubre los flujos críticos de los
dos roles.

## Punto de partida

- **Backend**: 369 tests. JaCoCo mide un 97,1 % de líneas y un 82,6 % de ramas, pero sólo
  informa; no hay umbral (`14-fase5.1` lo dejó para cuando la cobertura fuera el asunto).
- **Frontend**: 12 tests unitarios (Vitest), del cliente HTTP y de las fechas. Las pantallas
  sólo se han probado a mano, con Playwright CLI, en las fases 11 y 12.
- **No hay E2E.** El CI levanta el stack (job `docker-image`) y comprueba el `health`, nada más.

## Lo que dice la cobertura del backend

Casi todo lo que falta son ramas defensivas: nulos, zonas horarias vacías, fusiones de
intervalos que el dominio ya impide. Seis huecos, sin embargo, son comportamiento que nadie ha
probado, y cuatro de ellos tocan el contrato:

1. **`AUTH_WEAK_PASSWORD` se puede producir y no está documentado.** La validación del DTO
   comprueba de 8 a 200 caracteres, pero bcrypt sólo usa 72 bytes, y `PasswordPolicy` rechaza
   más. Una contraseña de 100 caracteres ASCII devuelve un `400` con un código que el contrato
   no menciona.
2. **`AUTH_ACCOUNT_NOT_ACTIVE` (403) tampoco está documentado.** Sale al verificar el correo o
   al restablecer la contraseña de una cuenta desactivada, con un enlace pedido antes de
   desactivarla. El spec no declara ese `403` en ninguna de las dos operaciones.
3. **El handler de `TokenReuseDetectedException` es código muerto**: `AuthController` captura
   esa excepción antes y responde `AUTH_SESSION_EXPIRED`.
4. **El bootstrap del admin no tiene test.** El del profesor sí
   (`TeacherBootstrapIdempotencyTest`).
5. **Pedir restablecer la contraseña de una cuenta desactivada** no envía nada, y ningún test lo
   comprueba. Es un comportamiento de seguridad.
6. **Las validaciones del perfil del profesor**: nombre o teléfono demasiado largos y zona
   horaria vacía. Desde la Fase 13 sólo se escriben por `PATCH /me`, y allí no hay test.

El `500` genérico del `GlobalExceptionHandler` tampoco está probado. Que no filtre detalles es
asunto de la auditoría de seguridad (Fase 15).

## Decisiones propuestas

1. **Playwright Test en `frontend/e2e/`**, sólo con Chromium. Los flujos del alumno se ejecutan
   con viewport de móvil, porque es donde lo usará (PWA). Los del profesor y el admin, con el de
   escritorio.
2. **Contra el stack real, sin simular la API**: Postgres, Mailpit y el backend en Docker, y el
   frontend con `next build` y `next start`. Lo que se prueba es la integración: cookies,
   CSRF, refresco de sesión y proxy. El contrato ya lo vigila la Fase 13.
3. **El correo se lee de verdad**, a través de la API de Mailpit. La verificación y el
   restablecimiento siguen el enlace que llega, no un token sacado de la base.
4. **Cada test crea sus datos.** Sólo se da por existente lo que crea el bootstrap: el
   profesor y el admin. Los alumnos llevan un correo único. Las clases van en un día propio de
   cada ejecución, con una excepción de horas extra. Así la suite también funciona contra el
   stack de desarrollo sin tocar la disponibilidad semanal de Daniel.
5. **El rate limiting se sube en el entorno E2E**, igual que en el perfil de test: toda la
   suite sale de la misma IP, y 20 peticiones por minuto a `/auth/*` la estrangularían.
6. **Se localiza por rol y nombre accesible** (`getByRole`, `getByLabel`), no por `data-testid`.
   Si una pantalla no se puede recorrer así, se arregla la pantalla: es un hueco de
   accesibilidad.
7. **Flujos cubiertos**:
   - *Alumno*: registro, correo, verificación y perfil; después ve la clase, reserva y cancela.
   - *Profesor*: disponibilidad, gestionar al alumno y crear la clase. Después ve la reserva,
     marca la asistencia y cancela la clase, y el alumno la ve cancelada.
   - *Plazas*: con la clase llena, otro alumno no puede reservar.
   - *Sesión*: recargar mantiene la sesión, cerrarla la termina, una ruta protegida lleva al
     login y un rol no entra en las pantallas de otro.
   - *Contraseña olvidada*: correo, enlace, contraseña nueva y login con ella.
   - *Admin*: cambia el límite de alumnos y desactiva a un alumno, que ya no puede entrar.

   Lo que depende del reloj (la ventana de 24 horas, las clases pasadas) no se puede controlar
   en el stack real. Ya lo prueba el backend con `Clock` fijo.
8. **En CI, el job `docker-image` pasa a ser "el stack arranca y los flujos E2E pasan"**: ya
   levanta el stack, así que no se duplica. Si falla, se publican el informe y la traza de
   Playwright.
   - Sin reintentos: un test que pasa a la segunda es un test que falla.
   - Un test saltado rompe el job, como en el backend.
9. **La cobertura del backend pasa a ser una puerta**, con `jacoco:check`, líneas y ramas del
   conjunto. El umbral es lo que se mida al terminar la fase, redondeado hacia abajo: no es una
   meta, es un suelo que impide retroceder. Subirlo es una decisión de otra fase.
10. **El frontend no tiene umbral de cobertura.** Instrumentar Next para medir los E2E cuesta más
    de lo que dice. Sus pantallas las cubren los flujos del punto 7.
11. **Los seis huecos se cierran con test**:
    - Los dos códigos se documentan en el spec y en `11-contrato-api.md`.
    - El handler muerto se borra.

## Fuera, y por qué

- **Incidencias del admin** (listar reservas y cancelar clases): sigue propuesta como fase corta
  12.1, que Daniel decide cuándo.
- **Otros navegadores y regresión visual**: Chromium basta para un MVP con un profesor.
  Firefox y WebKit se pueden añadir en la Fase 17, con la PWA.
- **El `500` sin detalles**: Fase 15.

## Criterios de aceptación

- `npm run e2e` pasa en local contra el stack y en CI, con 0 saltados y la lista de tests
  pegada.
- Comprobado rompiéndolo:
  - quitar el refresco de sesión del cliente hace fallar el test de recarga;
  - que el backend no envíe el correo de verificación hace fallar el registro;
  - bajar la cobertura por debajo del suelo rompe `mvn verify`.
- `mvn verify` en verde con `jacoco:check` activo y los seis huecos cerrados.
- `lint`, `typecheck`, `test` y `build` del frontend siguen en verde.

## Decisiones tomadas al implementar

1. **El suelo es 97 % de líneas y 83 % de ramas**, no el 97,1/82,6 del punto de partida ni el
   97,6/84,1 que salió después. Esas dos cifras estaban infladas:
   - JaCoCo acumulaba en `jacoco.exec` los datos de ejecuciones anteriores. Ahora cada ejecución
     empieza de cero (`append=false`).
   - `target/` conservaba `.class` de clases ya borradas.

   Medido con `mvn clean verify`, la suite completa da 97,5 % y 83,9 %.
2. **La asistencia se prueba con una clase que empieza a los 15 segundos.** Se crea por la API,
   fuera de horario a propósito, y el test espera a que empiece: el stack corre con el reloj
   real.
3. **Lo que el test no recorre se prepara por la API**, con el cliente tipado de `openapi.yaml`
   (`e2e/support/arrange.ts`), y se deshace al terminar:
   - alumnos que dejan de estar gestionados;
   - clases canceladas y excepciones borradas;
   - la configuración, devuelta a su valor.

   Las cuentas `e2e-*` se quedan, porque la API no borra usuarios.
4. **El "día libre" excluye también los días con clases canceladas**: siguen en el calendario
   del profesor y compartirían pantalla con las del test.
5. **Arreglado en el frontend**: el mensaje de `AUTH_WEAK_PASSWORD` pedía "al menos 10
   caracteres", pero desde la validación del DTO ese código sólo sale por una contraseña
   demasiado larga.
