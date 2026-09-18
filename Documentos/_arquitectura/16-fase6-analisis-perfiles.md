# Fase 6 — Análisis: perfiles y gestión de usuarios

Corresponde a la **Fase 2 del roadmap** (`09-roadmap-implementacion.md`), que numera las fases de
otra manera. Cubre los módulos `teacher` y `student`, y la relación "alumno gestionado".

Este documento es el entregable de análisis: no se escribe código hasta que esté validado.

## Alcance

| Endpoint | Quién | Qué hace |
|---|---|---|
| `GET /me` | Autenticado | Cuenta + datos personales del usuario |
| `PATCH /me` | Autenticado | Actualiza los campos personales aplicables a su rol |
| `GET /teacher/profile` | Autenticado | Perfil operativo del profesor único |
| `PATCH /teacher/profile` | `TEACHER` | Actualiza su propio perfil |
| `GET /teacher/students` | `TEACHER` | Lista de alumnos gestionados |
| `POST /teacher/students/{userId}/manage` | `TEACHER` | Asocia a un alumno registrado |
| `DELETE /teacher/students/{userId}/manage` | `TEACHER` | Desactiva la gestión |

Fuera de esta fase: disponibilidad, clases, reservas y la consola de administración.

## Decisiones

### La fase se parte en dos entregas

`student` depende de `teacher` (grafo de `02-arquitectura.md`), así que se construye en ese
orden y cada módulo entra por su propio Pull Request: `fase-6-teacher` y luego
`fase-6-student`. La fase se cierra cuando ambos están en `main` con el pipeline en verde.

### El bootstrap del profesor pasa a crear también su perfil

`10-diagrama-er.md` dice que `teacher_profiles` se rellena en el bootstrap inicial junto con la
fila de `users`. El `TeacherBootstrap` de la Fase 5 **solo crea la cuenta**, y como
`display_name` y `timezone` son `NOT NULL`, el profesor quedaría sin perfil y el endpoint
`GET /teacher/profile` no tendría nada que devolver.

El bootstrap pasa a crear ambas filas en la misma transacción, con dos variables de entorno
nuevas para el nombre visible y la zona horaria IANA. Sigue siendo idempotente, y el test que lo
demuestra se extiende para cubrir también el perfil.

Es una corrección de la fase anterior, no una funcionalidad nueva: se señala en lugar de
mantenerse por compatibilidad, como exige la metodología.

### `/me` lo compone un orquestador, no `identity`

`MeResponse` mezcla datos de `identity` (`id`, `email`, `role`, `status`, `emailVerifiedAt`) con
datos personales que viven en `student_profiles` o en `teacher_profiles`. Que `identity` lea esas
tablas rompería dos reglas a la vez: el acceso directo a la persistencia de otro módulo y la
dirección del grafo de dependencias, porque `identity` no debe depender de nadie.

Cada módulo expone un **puerto de consulta público** con los datos de perfil de un usuario, y un
adaptador web compone la respuesta llamando a los puertos que correspondan al rol. Ningún módulo
aprende nada de las tablas del otro.

### El perfil del alumno nace cuando el alumno lo rellena

El flujo de alta de `01-product-architect.md` es: registro → verificación → **completar datos
personales** → el profesor lo gestiona. La fila de `student_profiles` se crea en ese tercer paso,
con `PATCH /me`, no en el registro.

Consecuencias que hay que respetar en el contrato:

- `GET /me` de un alumno recién verificado devuelve los campos personales a `null`. El frontend
  usa esa ausencia para saber que debe pedirlos.
- `fullName` **no vuelve** a `RegisterRequest`. La nota que lo retiró en la Fase 5 sigue vigente.
- El primer `PATCH /me` de un alumno exige `fullName`, porque la columna es `NOT NULL`; los
  siguientes pueden ser parciales.

### Asociar a un alumno se hace por email exacto

`GET /teacher/students?query=` sirve hoy, según el propio contrato, para dos cosas distintas:
listar los alumnos ya gestionados y localizar a un alumno cualquiera del sistema. Lo segundo, con
búsqueda parcial, permite al profesor **enumerar quién está registrado en la plataforma** y leer
nombres de personas con las que no tiene ninguna relación. `08-security-engineer.md` prohíbe
exactamente eso.

Se separan los dos usos:

- **Buscar para asociar**: solo por **email exacto y completo**. Quien va a asociar a un alumno
  conoce su dirección; no necesita explorar. La respuesta se limita a lo imprescindible para
  identificarlo, sin DNI ni dirección.
- **Buscar entre los míos**: búsqueda parcial por nombre o email, restringida a los alumnos que
  ese profesor ya gestiona.

### El límite de alumnos se comprueba desde ahora

El límite configurable vive en `platform_configuration` y su consola no llega hasta la Fase 7 del
roadmap. Aun así, la comprobación se implementa ya, leyendo el valor con un defecto razonable:
añadirla después obligaría a volver a abrir el caso de uso y su transacción. Superar el límite
responde `422`.

### Desactivar un alumno: deuda explícita

La regla dice que desactivar a un alumno cancela sus reservas futuras activas. El módulo
`booking` no existe hasta la Fase 9, así que **en esta fase la desactivación no cancela nada**:
marca `teacher_students.status = 'INACTIVE'` y registra `deactivated_at`.

Queda anotado como deuda con destinatario: la Fase 9 debe cerrarla, y su criterio de salida
tendrá que incluir un test de que desactivar cancela las reservas futuras. No se deja un `TODO`
en el código sin dueño.

## Datos personales

`national_id` y `address` son de acceso restringido (`10-diagrama-er.md`, `08-security-engineer.md`):

- Solo se devuelven al propio usuario, al profesor que lo gestiona y a un `ADMIN`.
- Nunca aparecen en listados ni en la búsqueda por email.
- Nunca se escriben en logs, ni completos ni parciales.

## Criterios de aceptación

Medibles, según la regla 5 de la metodología. La fase no se cierra sin un test que demuestre cada uno:

1. Un alumno que pide el perfil de otro alumno recibe `403`, aunque conozca su UUID.
2. El profesor no ve `nationalId` ni `address` de un alumno que no gestiona.
3. Asociar dos veces al mismo alumno responde `409` y no crea una segunda fila.
4. `PATCH /me` no permite cambiar el rol, el email ni el estado de la cuenta, aunque lleguen en
   el cuerpo.
5. El bootstrap sigue siendo idempotente creando además el perfil del profesor.
6. La búsqueda para asociar no acepta coincidencias parciales.
7. Un alumno desactivado deja de figurar como gestionado y no puede ser reactivado por accidente
   creando una segunda relación.

## Estrategia de testing

Pirámide habitual —dominio sin Spring, aplicación con puertos simulados, REST, integración con
Testcontainers— más una novedad:

**Esta es la fase en la que deben aparecer los tests de ArchUnit.** `TennisPlatformApp/CLAUDE.md`
dice que las fronteras entre módulos "se harán cumplir con ArchUnit una vez exista código": hasta
ahora había un solo módulo y no había frontera que romper. Con `teacher` y `student` ya son tres,
con un grafo que respetar y un `/me` que tiene la tentación de saltárselo.

Reglas mínimas a verificar: que `domain` no dependa de Spring ni de JPA, que ningún módulo importe
`adapters` o entidades de otro, y que se respete la dirección del grafo.

## Aislamiento de los tests de integración

Los tests de integración comparten una base de datos mutable, y hasta ahora su resultado dependía
del orden de ejecución: en la Fase 5.1 eso rompió el pipeline en su primera ejecución, porque
surefire no ordena las clases igual en Windows que en Linux. Con un módulo se sostuvo limpiando a
mano; esta fase añade dos que escriben en `users`, `teacher_profiles`, `student_profiles` y
`teacher_students`.

**Decisión: la limpieza pasa a ser automática.** `AbstractIntegrationTest` vacía las tablas de
negocio antes de cada test, en orden de claves foráneas, de modo que ninguna clase pueda volver a
depender de lo que dejó otra. Se elige frente a un contenedor o un esquema por clase porque
aísla igual de bien para este caso y no multiplica el tiempo de la suite en CI.

Dos cuidados que la implementación debe respetar:

- La cuenta y el perfil del profesor los crea el bootstrap **al arrancar el contexto**, no antes
  de cada test. Si la limpieza los borra sin más, los tests que esperan un profesor existente
  fallarán a partir del segundo. La limpieza tiene que preservarlos o volver a sembrarlos.
- Las limpiezas manuales que añadimos en la Fase 5.1 —en `TeacherBootstrapIdempotencyTest` y en
  `theSchemaAllowsOnlyOneTeacher`— quedan redundantes y deben retirarse, para que no haya dos
  mecanismos compitiendo.
