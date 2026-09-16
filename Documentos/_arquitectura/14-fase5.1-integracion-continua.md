# Fase 5.1 — Integración continua

## Por qué esta fase existe y por qué aquí

La metodología original situaba la integración continua en la fase 15, junto al empaquetado de
despliegue. La regla adicional 3 de `12-metodologia-trabajo.md` la adelantó a 5.1: dejarla al
final significa descubrir al final si el proyecto es reproducible fuera de la máquina de
desarrollo. Colocada justo después de autenticación, todo el trabajo posterior —perfiles,
disponibilidad, clases, reservas— nace ya cubierto por el pipeline.

## Criterio de salida

Declarado antes de implementar, según la regla adicional 5:

> El pipeline falla si el contador `Skipped` de surefire es mayor que 0; existe un test que
> demuestra que ejecutar `TeacherBootstrap` dos veces no crea una segunda cuenta de profesor;
> y el job de imagen levanta el stack y obtiene un 200 del health endpoint.

Los tres se cumplen.

## Decisiones

### El pipeline es solo de backend

`06-devops-engineer.md` pide también lint, *type checking*, tests y E2E de frontend. No existe
`frontend/`: nace en la Fase 11. Se descartó crear *jobs* vacíos como marcador, porque un job
que no comprueba nada y sale en verde es peor que su ausencia — entrena a leer el check como
ruido. Los jobs de frontend se añadirán en la Fase 11, con algo real que ejecutar.

### `mvn verify` como único comando

El pipeline no encadena `mvn test`, luego el análisis, luego la cobertura: ejecuta `mvn verify`,
igual que un desarrollador en su máquina. Spotless corre en `validate`, SpotBugs y JaCoCo en
`verify`. Si el pipeline y el desarrollador ejecutan comandos distintos, acaban divergiendo y el
CI se convierte en un sitio donde aparecen fallos que nadie reproduce en local.

### Un test saltado rompe el build

Es la lección de la Fase 4 convertida en código. Aquella suite reportó `BUILD SUCCESS` con cinco
de ocho tests saltándose en silencio porque Testcontainers no alcanzaba Docker. Un paso del
pipeline lee los XML de surefire, suma los `skipped` y falla si hay alguno. Un test de
integración que se salta no prueba nada, pero *parece* que sí.

### Spotless sin formateador completo

Se descartó adoptar `google-java-format` o similar: reformatearía los 153 archivos existentes en
un único commit y enterraría la historia de las fases 4 y 5 bajo un diff cosmético. Las reglas
activas solo detectan lo objetivamente incorrecto —imports muertos, espacios finales, falta de
salto de línea final—. Detectó dos archivos guardados con CRLF mientras los otros 151 usaban LF.

### SpotBugs con umbral `Medium`

Con `Low` el análisis inunda de avisos de estilo un código joven, y el equipo aprende a ignorar
el check. Las exclusiones viven en `backend/spotbugs-exclude.xml` y **cada una lleva escrito su
motivo**: una exclusión sin justificación es indistinguible de un fallo que alguien decidió
dejar de ver.

### JaCoCo informa, no bloquea

Con un solo módulo implementado, cualquier umbral de cobertura sería un número arbitrario contra
el que pelearían las fases siguientes. La puerta de cobertura corresponde a la Fase 12, que es
donde la cobertura es el asunto. Ahora solo se publica el informe.

### Dependabot en lugar de OWASP Dependency-Check

Dependency-Check exige una clave de la NVD y añade minutos a cada build. Dependabot reporta las
mismas dependencias vulnerables fuera de banda, sin ralentizar el pipeline. Se agrupan las
actualizaciones menores y de parche en un único PR semanal; las mayores llegan sueltas, porque
hay que leerlas.

### La imagen se construye pero no se publica

No hay destino de despliegue: `06-devops-engineer.md` aplaza deliberadamente la elección de
proveedor hasta que haya usuarios reales. Publicar imágenes en un registro sería inventario que
nadie consume. Lo que sí debe estar protegido contra regresiones es que la imagen construya y
que la aplicación arranque de verdad contra una base de datos real con Liquibase aplicado, así
que el job levanta el stack completo y consulta el health endpoint desde fuera del contenedor.

## Dos defectos que encontró el análisis estático

Ninguno de los dos se silenció con una exclusión.

1. **Inyección en los logs y en la cabecera de respuesta** (`HRS_REQUEST_PARAMETER_TO_HTTP_HEADER`,
   `CorrelationIdFilter`). El `X-Correlation-Id` que enviaba el cliente se copiaba sin validar a
   la cabecera de respuesta y al MDC, es decir, a todas las líneas de log de esa petición. Un
   cliente podía inyectar saltos de línea y fabricar entradas de log falsas, o empujar una
   cabecera de 8 KB a cada línea. Ahora el valor entrante se descarta salvo que cumpla
   `[A-Za-z0-9._:-]{1,64}`; si no, se genera uno nuevo. Dos tests lo demuestran.
2. **Constructor que lanza en una clase extensible** (`CT_CONSTRUCTOR_THROW`, `JwtAccessTokens`).
   El constructor valida el secreto y puede lanzar, y una subclase con finalizador podía
   quedarse con una instancia a medio construir. La clase pasa a ser `final`: no hay razón para
   extender un adaptador de salida, ya que los colaboradores dependen de `AccessTokenIssuer`.

## Lo que queda fuera del repositorio

Dos cosas no pueden versionarse y hay que configurarlas en GitHub:

- **Proteger `main`** exigiendo que el check de CI esté en verde para poder fusionar. Sin esto el
  pipeline informa pero no impide nada.
- **Habilitar Dependabot** en la configuración de seguridad del repositorio.
