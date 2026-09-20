# Análisis — Forzar los límites modulares y hexagonales con ArchUnit

Este documento es el entregable de análisis: no se escribe código hasta que esté validado.

No corresponde a ninguna fase del roadmap. Es una corrección de deuda transversal que atraviesa
todas las fases restantes, y por eso se plantea como entrega propia y no como apéndice de la
Fase 6.

## Por qué ahora

`TennisPlatformApp/CLAUDE.md` afirmaba que los límites entre módulos *"se van a forzar con tests
de ArchUnit una vez exista el código"*. El código existía y ArchUnit no: ni en `pom.xml` ni un
solo test. La documentación describía un control que nadie ejecutaba.

> **Nota de estado (18/09/2026).** Este documento se escribió con ese punto de partida. Mientras
> se redactaba, el commit `3990eee` introdujo `archunit-junit5` y siete reglas iniciales junto al
> módulo `teacher`. Las decisiones de este análisis se aplicaron después sobre ellas, hasta las
> 21 reglas actuales. Lo que sigue se conserva porque el razonamiento es el que se acordó; el
> estado real está al final, en «Estado».

El momento importa. El reparto actual del backend es:

| Módulo | Clases (sin `package-info`) |
|---|---|
| `identity` | 68 |
| `teacher` | 20 |
| `shared` | 1 |
| `student`, `availability`, `lesson`, `booking`, `calendar`, `administration` | 0 (solo andamiaje) |

Seis de los nueve módulos están vacíos. Escribir las reglas ahora significa que cada módulo nace
ya vigilado; escribirlas después significa negociar con código que ya incumple.

### La Fase 6 ya contiene una violación corregida a mano

El commit `3990eee` movió `AuthenticatedUser` de
`identity/adapters/out/security/` a `identity/application/port/in/`.

Era necesario: `teacher/adapters/in/web/TeacherProfileController` lo importa, y mientras vivía en
`adapters/out/security` ese import era un módulo alcanzando el adaptador interno de otro —
exactamente lo que la arquitectura prohíbe. Lo cazó una persona leyendo el código. Es la clase de
error que una regla detecta en tres segundos y una revisión humana detecta cuando se fija.

## Estado de partida (verificado, no supuesto)

Antes de proponer reglas conviene saber cuáles se cumplen ya. Comprobado sobre el código actual:

| Invariante | Estado |
|---|---|
| `identity` no importa ningún otro módulo | Se cumple |
| `teacher` solo importa `identity` | Se cumple |
| Los cruces entre módulos pasan por `application/port/in` | Se cumple (2 imports, ambos por el puerto) |
| `domain/` no importa Spring, JPA ni Hibernate | Se cumple (cero imports de framework) |
| `domain/` y `application/` no importan `adapters/` | Se cumple |
| `application/` no importa Spring | **No se cumple**: usa `@Transactional` |

La última fila no es un defecto: `02-arquitectura.md` sitúa deliberadamente las fronteras
transaccionales en `application/service`. La regla tiene que permitirlo de forma explícita, o
fallaría el primer día y acabaría relajada a base de excepciones.

**Consecuencia:** salvo esa excepción declarada, las reglas propuestas pasan hoy en verde. Esta
entrega no arrastra una limpieza previa.

## Alcance

Cuatro grupos de reglas.

### A. Grafo de dependencias entre módulos

Las dependencias permitidas de `02-arquitectura.md`, codificadas y unidireccionales:

| Módulo | Puede depender de |
|---|---|
| `identity` | nada |
| `teacher` | `identity` |
| `student` | `identity`, `teacher` |
| `availability` | `teacher` |
| `lesson` | `teacher`, `availability` |
| `booking` | `student`, `lesson` |
| `administration` | `identity`, `teacher`, `student` |
| `calendar` | solo interfaces públicas de consulta de otros módulos |
| `shared` | nada |

Más la ausencia de ciclos entre módulos.

### B. Capas hexagonales dentro de cada módulo

- `domain` no depende de Spring, JPA, Hibernate ni de nada `web`.
- `domain` no depende de `application` ni de `adapters`.
- `application` no depende de `adapters`.
- `adapters/out/persistence` no es accesible desde fuera de su módulo.

### C. Acceso cruzado solo por puertos

Un módulo solo puede importar de otro lo que cuelgue de `application/port/in`. Quedan prohibidos
los imports a `domain`, `application/service`, `application/port/out`, `adapters` y
`configuration` ajenos.

### D. Higiene

- Ninguna entidad JPA se expone en una firma de controlador (los DTOs son obligatorios).
- Ninguna clase de `domain` lleva anotaciones de persistencia.

## Ambigüedades a resolver antes de escribir código

Son decisiones tuyas, no detalles de implementación. Ninguna tiene respuesta en los documentos
actuales.

### 1. `config`, `error` y `web` no son módulos

Existen tres paquetes de primer nivel bajo `com.tennisplatform` que **no aparecen en la lista de
nueve módulos** de `02-arquitectura.md`: `config` (1 clase), `error` (1) y `web` (1). Hoy nadie
los importa, pero una regla de "todo paquete de primer nivel es un módulo del grafo" los declara
violaciones al instante.

Opciones: (a) absorberlos en `shared`, coherente con "primitivas técnicas, sin lógica de
negocio"; (b) declararlos un núcleo técnico exento, del que cualquier módulo puede depender;
(c) dejarlos fuera del alcance de las reglas y documentarlo.

Mi recomendación es **(b)**, y documentarlo en `02-arquitectura.md`: `SecurityConfig` y
`GlobalExceptionHandler` son cableado de aplicación, no primitivas reutilizables, y meterlos en
`shared` desdibuja lo que `shared` significa.

### 2. Qué hacer con los seis módulos vacíos

¿Las reglas se escriben ya para los nueve, o solo para los que tienen código? Escribirlas para
los nueve es más trabajo ahora y cero después. Escribirlas incrementalmente reparte el esfuerzo
pero repite la situación actual: reglas que la documentación da por hechas y no existen.

Recomiendo **los nueve desde el principio**. El grafo ya está decidido y no depende de que el
código exista.

### 3. `calendar` necesita una definición operable

"Solo interfaces públicas de consulta" no es comprobable tal como está escrito. Hay que fijar un
criterio mecánico, por ejemplo: `calendar` solo puede importar tipos de `application/port/in`
cuyo nombre empiece por `Get`, `Find` o `Query`, y nunca tipos con `Create`, `Update`, `Delete`
o `Cancel`. Es una convención de nombres, y conviene decidirla explícitamente porque condiciona
cómo se nombran los puertos de todos los demás módulos.

### 4. Dónde corren estas reglas

El pipeline **falla si algún test se salta**. Los tests de ArchUnit no tocan base de datos ni
levantan contexto de Spring, así que deben ejecutarse siempre y no pueden depender de Docker.
Propongo que sean tests unitarios normales dentro de `mvn test`, sin perfil ni etiqueta propia:
cualquier mecanismo que permita saltarlos reproduce el fallo silencioso de la Fase 4.

## Criterios de aceptación

La entrega se da por cerrada cuando:

1. `archunit-junit5` está en `pom.xml` con versión fija y la justificación de por qué.
2. Existen tests que cubren los cuatro grupos A–D, cada uno con su nombre descriptivo.
3. La excepción de `@Transactional` en `application` está declarada en el propio test, con su
   porqué escrito al lado y no como una exclusión muda.
4. `mvn verify` pasa en verde **y** el contador de `Skipped:` es cero, con la salida pegada como
   evidencia.
5. Se demuestra que las reglas muerden: se introduce a propósito un import prohibido, se pega la
   salida del fallo, y se revierte. Una regla que nunca se ha visto fallar no está probada.
6. `TennisPlatformApp/CLAUDE.md` deja de decir "una vez exista el código" y pasa a describir lo
   que hay.
7. Las decisiones de las cuatro ambigüedades quedan escritas en `02-arquitectura.md`.

## Fuera de alcance

Reglas de nomenclatura general, límites de complejidad ciclomática, control de dependencias de
terceros y cualquier regla sobre el frontend. Este trabajo cubre exclusivamente los límites
modulares y las capas hexagonales del backend.

## Decisiones tomadas

Las cuatro ambigüedades quedaron decididas el 18 de septiembre de 2026:

1. **`config`, `error` y `web` son núcleo técnico exento**, fuera del grafo. Cualquier módulo
   puede usarlos y el *composition root* puede ver cualquier módulo, porque ensamblar
   implementaciones concretas es exactamente su función. No se absorben en `shared`: `shared`
   son primitivas reutilizables, no cableado de aplicación.
2. **Las reglas se escriben para los nueve módulos**, incluidos los seis vacíos. El grafo ya
   está decidido y no depende de que exista el código.
3. **`calendar` solo puede usar puertos cuyo nombre empiece por `Get`, `Find` o `Query`.** Los
   tipos de datos que devuelven esos puertos —vistas y records— no son interfaces y quedan fuera
   de la regla. Esto fija la convención de nombres de los puertos de todos los módulos.
4. **Las reglas corren como tests unitarios normales dentro de `mvn test`**, sin perfil ni
   etiqueta: no levantan Spring ni tocan Docker, así que no pueden saltarse. Cualquier mecanismo
   que permitiera saltarlas reproduciría el fallo silencioso de la Fase 4.

## Estado

**Implementado y verificado** en la rama `fase-6-teacher`, junto al módulo `teacher`, en lugar
de como entrega separada: el fichero de reglas ya vivía en esa rama y dos PRs habrían chocado en
él.

`ModuleBoundariesTest` contiene **21 reglas** que cubren los cuatro grupos A–D. La excepción de
`@Transactional` en `application` está declarada en el propio test con su porqué al lado, no
como una exclusión muda.

Evidencia de que las reglas muerden (criterio 5), introduciendo a propósito una comparación
contra `identity.domain.Role` en `TeacherProfileController` y revirtiéndola después:

```
Architecture Violation - Rule 'no classes that reside in a package 'com.tennisplatform.teacher..'
should depend on classes that the insides of a module other than teacher, because a module's
public API is its inbound ports, nothing else' was violated (1 times):
  TeacherProfileController.update(...) gets field <com.tennisplatform.identity.domain.Role.TEACHER>
  in (TeacherProfileController.java:48)
```

Esa violación es también la razón de que `AuthenticatedUser` ofrezca `isTeacher()`: sin él,
cualquier módulo que necesite comprobar el rol tendría que importar el dominio de `identity`.

## Actualización de la Fase 6 (entrega `student`)

Las reglas pasan de 21 a 24, por tres motivos:

1. **Módulo `platform`.** Entra en `MODULES` y recibe sus dos reglas: no depende de ningún
   módulo (`platformDependsOnNoModule`) y solo se le accede por sus puertos de entrada
   (`platformCrossesOnlyThroughPorts`). `student` pasa a poder depender de él.
2. **`web` deja de ser una exención sin vigilancia.** `theWebEdgeOnlyUsesInboundPorts` permite
   al borde de composición llamar a `application/port/in` de cualquier módulo y nada más: ni
   dominios, ni servicios, ni adaptadores, ni entidades. Es lo que hace que `/me` pueda componer
   tres módulos sin que `web` se convierta en la puerta trasera por la que dejan de aplicarse
   las fronteras.

La regla de `web` tuvo un efecto de diseño inmediato: `UserSummary` devolvía `Role` y
`UserStatus`, los enums del dominio de `identity`. Cualquiera que leyera esa vista desde fuera
dependía de ese dominio. Ahora viajan como texto, igual que `TeacherProfileView` convierte el
`ZoneId` en su id IANA. Una vista lleva valores de cable, no tipos de dominio.
