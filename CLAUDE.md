# CLAUDE.md — Tennis Platform

Guía de entrada al repositorio. Si abres el proyecto en esta carpeta, empieza por aquí.
El detalle técnico del backend está en `TennisPlatformApp/CLAUDE.md`.

## Qué es

Plataforma web/PWA para que un profesor de tenis gestione alumnos, disponibilidad, clases y
reservas. MVP con un único profesor y una única pista. Roles: `ADMIN`, `TEACHER`, `STUDENT`.

## Cómo se trabaja aquí

**Lee `Documentos/_arquitectura/12-metodologia-trabajo.md` antes de tocar nada.** Define el
proceso acordado y no es opcional. Lo esencial:

- **No se avanza de fase sin validación explícita del usuario.** No encadenes fases.
- En las fases de análisis y diseño **no se escribe código**: primero requisitos,
  ambigüedades, decisiones explicadas, contratos y criterios de aceptación.
- **La evidencia se pega, no se afirma.** Una fase no se da por terminada sin la salida real
  del comando, y hay que mirar el contador de tests **saltados**, no solo `BUILD SUCCESS`.
- **Cada fase cierra con su commit.**
- Si detectas una mala decisión anterior, **señálala y propón alternativa**; no la mantengas
  por compatibilidad. Ya ha pasado tres veces y las tres eran correcciones necesarias.
- Al terminar cada bloque grande de trabajo, entrega un **resumen de las decisiones tomadas y
  su porqué**. No narres cada archivo mientras trabajas.

Los documentos de `Documentos/_arquitectura/` son vinculantes, no lectura de fondo.

## Estado por fases

| Fase | Estado |
|---|---|
| 1. Análisis funcional | Completada |
| 2. Arquitectura | Completada |
| 3. Modelo de datos | Completada |
| 4. Skeleton del backend | Completada y verificada |
| 5. Seguridad y autenticación (`identity`) | Completada — ver salvedades |
| **5.1 Integración continua** | **Siguiente** |
| 6 en adelante | Pendientes |

### Salvedades abiertas de la Fase 5

Dos criterios de aceptación no quedaron cerrados del todo:

1. **El bootstrap del profesor no tiene test de idempotencia.** Está implementado en
   `TeacherBootstrap` y funciona, pero nada demuestra que ejecutarlo dos veces no cree una
   segunda cuenta.
2. **El arranque con `docker compose` se verificó antes de los últimos commits.** Conviene
   repetirlo antes de dar la fase por cerrada.

## Comandos

```
cd TennisPlatformApp/backend
mvn test                  # suite completa; revisa el contador de "Skipped"
mvn clean package
```

```
cd TennisPlatformApp
docker compose up postgres    # solo la base, para ejecutar el backend desde el IDE
docker compose up -d --build  # stack completo
```

## Entorno de la máquina de desarrollo

- **El backend escucha en el puerto 8081**, no en el 8080: ese está ocupado por un Tomcat 8
  ajeno al proyecto. Configurado en `TennisPlatformApp/.env`.
- `JAVA_HOME` apunta al JDK 21; `java` en el PATH también es el 21.
- Buzón de correo de desarrollo (Mailpit) en **http://localhost:8025**. Ahí aparecen los
  emails de verificación y recuperación.
- PostgreSQL en `localhost:5432`, base/usuario/contraseña `tennis_platform`.

## Trampas conocidas

Cosas que ya han costado tiempo y que fallan **en silencio**:

- **`testcontainers.version` está fijada en `pom.xml`** por encima de la del BOM de Spring
  Boot. Docker Engine 29 rechaza las versiones antiguas de su API, y el síntoma no es un
  fallo: los tests de integración se **saltan** mientras el build sigue en verde.
- **`AbstractIntegrationTest` usa el patrón singleton container** (se arranca una vez y no se
  para nunca; lo limpia Ryuk). Cambiarlo a `@Container` apaga la base al terminar la primera
  clase de test y todas las siguientes fallan con `Failed to obtain JDBC Connection`.
- **`out` es un paquete de código**, no un directorio de compilación: `application/port/out` y
  `adapters/out`. Por eso `.gitignore` ancla esa regla a la raíz. Una regla `out/` suelta
  excluye medio módulo de cada commit sin avisar.
- **El rate limiting está subido en el perfil de test.** Todos los tests llaman desde
  `127.0.0.1` y se estrangularían entre ellos. `AuthRateLimitTest` lo baja y además desactiva
  los reintentos del cliente HTTP, porque Apache HttpClient respeta `Retry-After` y reintenta
  el 429 cuando el margen ya se ha repuesto.
- **Revocar y lanzar excepción en el mismo método transaccional deshace la revocación.** Pasó
  con la detección de reutilización de refresh tokens: hace falta `noRollbackFor`.

## Repositorio

`https://github.com/SFDK1990/tennis-platform` (privado). La rama de trabajo es `main`.
