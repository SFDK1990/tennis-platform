# Tennis Platform — Convenciones de código y de trabajo

## Propósito

Los demás documentos de esta carpeta definen *qué* se construye y `12-metodologia-trabajo.md`
define *en qué orden*. Este define **cómo se escribe**: idioma, comentarios, nombres, uso del
control de versiones y del utillaje de calidad.

Existe porque hasta la Fase 5.1 estas reglas no estaban escritas en ninguna parte: sobrevivían
únicamente mientras alguien leyera el código existente y lo imitara. Eso funciona con un módulo
y deja de funcionar en cuanto entra el frontend o pasa el tiempo suficiente.

Las decisiones de este documento fueron acordadas con Daniel el 18 de septiembre de 2026 y
tienen el mismo rango que el resto de documentos de esta carpeta.

## Idioma

| Qué | Idioma |
|---|---|
| Identificadores (clases, métodos, variables, tablas, columnas) | Inglés |
| Comentarios y Javadoc | Inglés |
| Mensajes de commit y descripciones de PR | Inglés |
| Contrato de API: rutas, campos JSON, códigos de error | Inglés |
| Documentos de `Documentos/_arquitectura/` | Español |
| Conversación con Daniel | Español |

El código va en inglés porque ya lo está —135 archivos de las fases 4 y 5— y porque los nombres
del framework, de JPA y de las librerías son ingleses de forma inevitable: mezclar idiomas
produce cosas como `guardarUser` o `RepositorioDeRefreshToken`. La documentación narrativa va en
español porque su lector es Daniel y su propósito es entender decisiones, no ejecutarse.

Cuando la Fase 11 traiga el frontend, la misma regla se aplica a TypeScript. Los textos que ve
el usuario final son otra cosa: son contenido, no código, y su idioma se decidirá en esa fase.

## Comentarios: el porqué, nunca el qué

Un comentario que repite lo que el código ya dice es ruido que además envejece mal. Se comenta
lo que el código **no puede** decir: la razón de una decisión, la alternativa descartada, la
trampa que costó una tarde, el motivo por el que algo que parece raro es deliberado.

Bueno — explica un fallo silencioso que nadie deduciría leyendo la línea:

```xml
<!--
    Overrides the version managed by the Spring Boot BOM. Docker Engine 29 dropped
    support for old Docker API versions, which the older docker-java shipped with
    Boot 3.3.x still negotiates - it fails with HTTP 400 and every Testcontainers
    test silently skips.
-->
<testcontainers.version>1.21.4</testcontainers.version>
```

Bueno — justifica una decisión de diseño y advierte de la consecuencia de deshacerla:

```java
/**
 * Singleton container pattern on purpose: the container is started once for the whole
 * test run and never stopped. Using {@code @Container} instead would stop it at the end
 * of the first test class, leaving every later class with a dead database.
 */
```

Inútil — repite el código y no aporta nada:

```java
// Guarda el usuario
users.save(user);
```

Peligroso — describe un comportamiento que puede dejar de ser cierto sin que nadie lo note:

```java
// Devuelve siempre 200
```

Regla práctica: si el comentario se puede deducir leyendo la línea siguiente, sobra. Si explica
por qué esa línea es como es, se queda.

## Nombres de tests

Frase descriptiva en inglés, sin prefijos ni sufijos ceremoniosos:

```java
void runningTheBootstrapAgainNeverCreatesASecondTeacher()
void replacesAnIncomingCorrelationIdThatCouldForgeLogLinesOrSplitTheResponse()
void theSchemaAllowsOnlyOneTeacher()
```

El objetivo es que el informe de fallos se lea como una lista de afirmaciones sobre el sistema.
Un nombre como `testBootstrap2` obliga a abrir el archivo para saber qué se ha roto.

Los tests que cubren un criterio de aceptación de una fase lo indican en su Javadoc
(`/** Criterion 4: the schema itself refuses a second teacher. */`), de modo que el criterio y
su prueba no puedan separarse.

## Control de versiones

### Ramas y Pull Requests

**Cada fase se desarrolla en su propia rama y entra en `main` mediante Pull Request con el CI en
verde.** Desde la Fase 5.1 existe un pipeline, y un pipeline que solo informa después de que el
código ya está dentro no protege nada.

Nombre de rama: `fase-<n>-<tema>` para las fases, por ejemplo `fase-6-perfiles-usuarios`. Para
lo que no es una fase —documentación, correcciones sueltas— se usa `docs/<tema>` o
`fix/<tema>`.

Esto sustituye a la práctica de las fases 1 a 5.1, que se empujaron directamente a `main`. El
cambio exige, además, proteger `main` en GitHub exigiendo los dos checks del pipeline.

### Commits

Una fase puede —y suele— necesitar **varios commits**, con una condición: **cada commit debe
dejar el proyecto compilando y con los tests en verde**. Un commit no es un punto de guardado; es
una unidad que alguien puede revisar o revertir por separado.

Esto matiza la regla 2 de `12-metodologia-trabajo.md` ("cada fase cierra con un commit"): lo que
se exige es que la fase **cierre** con su commit de cierre, no que sea el único.

### Formato del mensaje: Conventional Commits

La cabecera sigue el estándar [Conventional Commits](https://www.conventionalcommits.org):

```
<tipo>(<ámbito>): <resumen en imperativo y en inglés>
```

Tipos admitidos:

| Tipo | Para qué |
|---|---|
| `feat` | Funcionalidad nueva visible para algún usuario del sistema |
| `fix` | Corrección de un defecto |
| `docs` | Documentación, incluidos los `CLAUDE.md` y esta carpeta |
| `test` | Tests que se añaden o arreglan sin tocar el código de producción |
| `refactor` | Cambio interno que no altera el comportamiento observable |
| `build` | Maven, dependencias, Dockerfile, compose |
| `ci` | Pipeline y configuración de GitHub Actions |
| `chore` | Mantenimiento que no encaja en lo anterior |

El **ámbito** es el módulo (`identity`, `teacher`, `student`, `availability`, `lesson`,
`booking`, `calendar`, `administration`, `shared`) o el área afectada (`backend`, `frontend`,
`compose`). Es opcional cuando el cambio es transversal.

Un cambio que rompe el contrato de API o el esquema lleva `!` antes de los dos puntos
(`feat(booking)!: ...`) y explica la ruptura en el cuerpo.

Se adopta porque a partir de la Fase 6 el historial pasa a construirse con ramas y PRs, y
conviene distinguir de un vistazo una corrección de una funcionalidad o de un cambio de
documentación, sin abrir cada commit.

### El cuerpo del mensaje

La cabecera dice *qué*; el cuerpo tiene que decir **por qué**, y es la parte que de verdad
importa. Se escribe en inglés, en prosa, y explica la razón del cambio, la alternativa
descartada si la hubo, y el problema que resuelve.

El commit de cierre de fase termina con la evidencia real de la verificación —la salida del
comando, con su recuento de tests ejecutados y **saltados**—, nunca con una afirmación de que
todo funciona.

Ejemplo completo:

```
fix(identity): stop trusting the client's correlation id

The incoming X-Correlation-Id was copied into a response header and into the
MDC, so a client could forge log lines with CR/LF. It is now discarded unless
it matches [A-Za-z0-9._:-]{1,64}.

Verification: mvn verify -> Tests run: 64, Failures: 0, Skipped: 0.
```

Los commits anteriores al 18 de septiembre de 2026 usan prosa sin prefijo. **No se reescriben**:
el historial ya está publicado y reescribirlo por cosmética es peor que convivir con dos
estilos claramente separados por una fecha.

## Utillaje de calidad

Todo se ejecuta con un único comando, el mismo en local y en CI:

```
cd TennisPlatformApp/backend
mvn verify
```

| Herramienta | Cuándo | Qué hace | Si falla |
|---|---|---|---|
| Spotless | fase `validate` | Imports muertos, espacios finales, salto de línea final | `mvn spotless:apply` |
| Surefire | fase `test` | La suite completa | Mirar también el contador de **saltados** |
| SpotBugs | fase `verify` | Análisis estático, umbral `Medium` | Arreglar; excluir es la excepción |
| JaCoCo | fase `verify` | Informe de cobertura, sin umbral | No rompe el build |

Dos reglas sobre el análisis estático:

1. **Un hallazgo se arregla; silenciarlo es la excepción.** Los dos primeros que encontró
   SpotBugs en la Fase 5.1 eran defectos reales y se corrigieron. Si un hallazgo se excluye, se
   hace porque el detector no puede ver la mitigación, no porque moleste.
2. **Todas las exclusiones viven en `backend/spotbugs-exclude.xml`, y cada una lleva escrito su
   motivo.** No se usan anotaciones en el código: una exclusión desperdigada no se audita, y una
   exclusión sin justificación es indistinguible de un fallo que alguien decidió dejar de ver.
   Cuando la mitigación exista, la exclusión debe nombrar el test que la demuestra.

## Reglas heredadas que siguen vigentes

No se repiten aquí porque ya están escritas, pero forman parte de las convenciones:

- Los DTO son siempre distintos de las entidades de dominio y de JPA (`02-arquitectura.md`).
- El acceso entre módulos pasa por los puertos públicos, nunca por las entidades o repositorios
  de otro módulo (`TennisPlatformApp/CLAUDE.md`).
- Los changelogs de Liquibase son *append-only*: un changeset ejecutado no se edita nunca; las
  correcciones son changesets nuevos (`05-database-engineer.md`).
- Nunca se registran en el log contraseñas, tokens, documentos de identidad completos ni
  direcciones (`08-security-engineer.md`).
