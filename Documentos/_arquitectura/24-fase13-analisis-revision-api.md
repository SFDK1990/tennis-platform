# Fase 13 — Análisis: revisión de la API

Criterio de salida (`12-metodologia-trabajo.md`): revisión REST completa y `openapi.yaml` sin
diferencias con lo implementado.

## Lo que se midió

"Sin diferencias" hay que medirlo, no afirmarlo. Se hizo un experimento, sin commitear: se
validó contra `openapi.yaml` **cada respuesta** de los 356 tests (swagger-request-validator, que
además rechaza los campos que el spec no declara).

- **Endpoints**: los 34 del spec existen en el código y viceversa, y los tests los llaman todos.
- **Cuerpos**: ningún campo de más, de menos o con otro tipo, salvo uno.
- **Diferencias reales, cinco**:
  1. `GET /teacher/students/lookup` devuelve `managedStatus: null` y el spec no lo admite:
     `nullable` junto a un `allOf` no tiene efecto en OpenAPI 3.0.3. Los tipos del frontend
     tampoco dicen que pueda ser `null`.
  2. `429` sin documentar en `/auth/forgot-password`. El rate limiting cubre todo `/auth/*` y
     sólo 3 de las 8 operaciones lo dicen.
  3. `401` sin documentar en `/teacher/students` y `/teacher/availability`. Pasa en casi todas:
     sólo 8 operaciones protegidas lo declaran.
  4. `400` sin documentar para un id mal formado (`/lessons/not-a-uuid`). Vale para todo `{id}`.
- **Al revés**: 29 respuestas documentadas que ningún test produce. Algunas faltan por probar,
  por ejemplo el `200` de `GET /admin/configuration`, los `403` de rol en varias operaciones o
  `409` en `verify-email`. Otras quizá no puedan ocurrir. La lista está al final.

## Decisiones propuestas

1. **El contrato lo vigilan los tests, siempre.** `AbstractIntegrationTest` valida cada
   respuesta contra `openapi.yaml`, y una diferencia hace fallar el test que la produjo. Sólo se
   validan respuestas: muchos tests mandan peticiones inválidas a propósito. La única excepción
   es el test que llama a una ruta inexistente, y va nombrada.
2. **Un test compara las rutas del código con las del spec**, en los dos sentidos, a partir de
   los mappings de Spring. Un endpoint nuevo sin documentar, o uno documentado que nadie
   implementa, rompe el build.
3. **Los estados comunes se documentan en todas partes.**
   - `401` en toda operación autenticada.
   - `429` en todo `/auth/*`.
   - `400` en toda ruta con `{id}`.

   Un test sobre el propio spec comprueba los dos primeros, que son los que se olvidan.
4. **Las 29 respuestas no observadas**: cada una recibe un test, o sale del spec si no puede
   ocurrir. Se mide una vez en esta fase, no en cada build. Hacerlo en cada build obligaría a
   agregar resultados de todas las clases de test, y eso es frágil.
5. **Se retira `PATCH /teacher/profile`.** Duplica `PATCH /me`, que es el que usa el frontend.
   Dos caminos para escribir lo mismo son dos sitios donde las reglas pueden divergir.
   `GET /teacher/profile` se queda: cualquier usuario lo lee para saber la zona horaria.
6. **Se retira `GET /teacher/lessons`.** Su propia descripción dice que existía "hasta que exista
   `/calendar`", y el frontend no lo usa. Sus tests pasan a `/calendar`.
7. **Las acciones siguen siendo acciones**: `/cancel`, `/manage`, `/attendance` y `/status`.
   Cancelar no es `DELETE`, porque la reserva sigue existiendo con su motivo. Tampoco es un
   `PATCH` de `status`, porque tiene reglas propias (ventana de 24 horas, quién cancela). La
   convención se escribe en `11-contrato-api.md`.
8. **El spec deja de ser un borrador**: `info.version: 1.0.0`, y el servidor pasa a ser
   relativo (`/api/v1`), porque hoy apunta al 8080 y el backend está en el 8081. Se limpia la
   introducción de `11-contrato-api.md`, que aún habla de mover el fichero.

## Fuera, y por qué

- **Incidencias del admin.** El spec y el código permiten al admin cancelar cualquier reserva,
  pero no tiene cómo listarlas (`GET /bookings` le da 403). Tampoco puede cancelar clases, cosa
  que `01` §17 sí prevé. Es funcionalidad nueva, no revisión. Propongo una fase corta propia
  (12.1) con su pantalla.
- **El nombre del alumno en la reserva.** El frontend ya lo resuelve con la lista de alumnos del
  profesor, así que no es una diferencia con el contrato.
- **La auditoría formal**: fuera del MVP (`01` §16).

## Criterios de aceptación

- `mvn verify` en verde con la validación activa en todos los tests de integración, y 0
  saltados.
- Comprobado rompiéndolo:
  - un campo sin declarar en un DTO hace fallar los tests de ese endpoint;
  - una operación borrada del spec hace fallar el test de rutas;
  - quitar un `401` del spec hace fallar el test de estados comunes.
- Los tipos del frontend se regeneran y `lint`, `typecheck`, `test` y `build` siguen en verde.
- Se repite la medición de respuestas no observadas y queda en cero, o con cada excepción
  justificada.

## Decisiones tomadas al implementar

1. **El `400` cubre toda operación que recibe entrada**, no solo las rutas con `{id}`. Un
   parámetro de query mal formado o un cuerpo ilegible también dan `400 VALIDATION_ERROR`, así que
   la regla que comprueba el test es "cuerpo, id o parámetros".
2. **`POST /bookings/{id}/cancel` pierde su `403`.** Los tres roles tienen su camino para
   cancelar, así que no puede ocurrir, y el spec no debe prometerlo.
3. **El validador no se apoya en el request factory.** `AuthRateLimitTest` instala el suyo, y con
   el buffering en el factory el interceptor se comía el cuerpo de la respuesta. Ahora el propio
   interceptor devuelve la respuesta ya leída.
4. **`allOf` se resuelve antes de validar.** Sin eso, cada parte de un `allOf` rechaza las
   propiedades de la otra: el validador trata las propiedades no declaradas como error, y eso es
   justo lo que queremos para detectar campos que se escapan.
5. **Las respuestas no observadas quedan en 38, todas de los estados comunes**:
   - `401`, que lo da la cadena de seguridad igual en toda ruta protegida;
   - `429`, que lo da el filtro de `/auth/*`;
   - `400`, que lo da el manejador global.

   Cada mecanismo tiene su test. Las 28 respuestas específicas de la lista inicial (29 menos la
   del endpoint retirado) tienen ahora test, o salieron del spec.

## Respuestas documentadas que ningún test produce (antes de la fase)

```
POST   /auth/register                        400, 429
POST   /auth/login                           429
POST   /auth/verify-email                    400, 409
POST   /auth/verification-email              429
POST   /auth/reset-password                  400, 409
PATCH  /me                                   401
GET    /teacher/students                     403
GET    /teacher/students/lookup              401
GET    /teacher/students/{userId}            401
POST   /teacher/students/{userId}/manage     401, 404
DELETE /teacher/students/{userId}/manage     401, 403
POST   /teacher/availability/exceptions      400, 403
DELETE /teacher/availability/exceptions/{id} 403
GET    /teacher/lessons                      403   (se retira)
GET    /bookings                             403
POST   /bookings/{id}/cancel                 403
POST   /teacher/lessons/{id}/cancel          403, 404
POST   /teacher/lessons/{id}/attendance      400, 404, 409
GET    /admin/configuration                  200
PATCH  /admin/users/{id}/status              400
```
