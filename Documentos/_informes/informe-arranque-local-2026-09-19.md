# Informe — arranque local y pruebas manuales (19/09/2026)

Fichero de trabajo, no versionado y borrable. Recoge lo que se hizo en la sesión del 19/09.

## Dónde está el proyecto

`main` está en el merge del PR #13 (`fase-6-teacher`). La Fase 6 va partida en dos entregas y
solo está hecha la primera.

| Fase | Estado |
|---|---|
| 1–5.1 | Completadas |
| 6 · `teacher` | En `main` (PR #13) |
| **6 · `student`** | **Siguiente: no empezada** |
| PR #14 (docs de límites de iniciativa) | Abierto, CI en verde, pendiente de fusionar |

El siguiente paso es la rama `fase-6-student`, que según `Documentos/_arquitectura/16-fase6-analisis-perfiles.md`
trae `GET/PATCH /me`, el perfil de alumno, `GET /teacher/students` y asociar/desactivar alumno.
Con la decisión tomada al final de la sesión anterior, incluye además el módulo nuevo `platform`
con **solo el lado de lectura** del límite de alumnos —tabla, puerto `GetStudentLimit`, servicio y
adaptador; la consola sigue siendo Fase 7— más la corrección de `02-arquitectura.md`, la lista
`MODULES`, `ModuleBoundariesTest` y `TennisPlatformApp/CLAUDE.md`.

Nada de eso está empezado: no se avanza de entrega sin validación.

## Arranque local

El stack llevaba dos días corriendo con una imagen **anterior al módulo `teacher`**: la base de
datos no tenía siquiera la tabla `teacher_profiles`. Se reconstruyó con
`docker compose up -d --build` y se añadieron al `.env` local —ignorado por git, con copia previa
en `.env.bak-20260919-…`— las variables que faltaban: `JWT_SECRET`, los puertos de Mailpit y las
credenciales del bootstrap del profesor, sin las cuales no se crea ningún profesor.

```
tennis-backend    Up (healthy)   0.0.0.0:8081->8080/tcp
tennis-mailpit    Up (healthy)   1025, 8025
tennis-postgres   Up (healthy)   5432

{"status":"UP","groups":["liveness","readiness"]}
TeacherBootstrap - Teacher account and profile created by bootstrap
```

Credenciales de desarrollo:

- Profesor: `profesor@tennisplatform.local` / `ProfesorDev2026!`
- Alumno de prueba: `alumno1@tennisplatform.local` / `NuevaClave2026!`
  (la cambió el propio flujo de recuperación durante las pruebas)
- Buzón de correo: http://localhost:8025

## Pruebas manuales contra la API

Todo lo que existe hoy responde como dice el contrato.

| Prueba | Resultado |
|---|---|
| Login profesor → `/me` → `GET /teacher/profile` | 200, rol `TEACHER`, perfil creado por el bootstrap |
| `PATCH /teacher/profile` (nombre, teléfono) | 200, persiste |
| `PATCH` con `timezone: Marte/Olympus` | 400 `TEACHER_PROFILE_INVALID` |
| Registro de alumno | 202 + email en Mailpit |
| Contraseña de 5 caracteres | 400 `VALIDATION_ERROR` |
| Verificar email / reutilizar el mismo enlace | 204 / 409 `AUTH_INVALID_TOKEN` |
| Alumno hace `PATCH /teacher/profile` | 403 `TEACHER_FORBIDDEN` |
| Token manipulado | 401 |
| Rotación de refresh + reutilización del viejo | rota; la reutilización revoca toda la familia (el nuevo también pasa a 401) |
| Logout → refresh | 204 → 401 |
| `forgot-password` con email inexistente | 202 igual, no filtra si la cuenta existe |
| Reset de contraseña | 204; la contraseña vieja deja de valer |
| 12 logins fallidos seguidos | `401 401 401 401 401 429 429 429 429 429 429 429` |

Esquema real leído de la base después de la migración:

```
                     Table "public.teacher_profiles"
    Column    |           Type           | Nullable | Default
--------------+--------------------------+----------+---------
 user_id      | uuid                     | not null |
 display_name | character varying(255)   | not null |
 phone        | character varying(30)    |          |
 timezone     | character varying(60)    | not null |
 created_at   | timestamp with time zone | not null | now()
 updated_at   | timestamp with time zone | not null | now()
Indexes:
    "teacher_profiles_pkey" PRIMARY KEY, btree (user_id)
Foreign-key constraints:
    "teacher_profiles_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(id)
```

## Dos hallazgos, sin corregir (decisión pendiente)

1. **CSRF no está documentado.** `/auth/refresh` y `/auth/logout` exigen la cabecera
   `X-XSRF-TOKEN`; es correcto —CSRF está activo justo donde la cookie sola autentica— pero no
   aparece ni en `TennisPlatformApp/openapi.yaml` ni en `Documentos/_arquitectura/11-contrato-api.md`.
   El frontend de la Fase 8 se estrellará contra un 403 sin saber por qué.
2. **Los errores del filtro de seguridad no siguen el contrato de errores.** Ese 403 y el 401 salen
   con el formato por defecto de Spring (`{"timestamp":…}`) o con cuerpo vacío, no con
   `application/problem+json` como promete `11-contrato-api.md`.

## Configuraciones de ejecución de IntelliJ

Creadas en `.idea/runConfigurations/` (locales: `.idea/` está en `.gitignore`). Aparecen en el
desplegable de ejecución al reabrir el proyecto.

| Configuración | Qué hace |
|---|---|
| **Backend (dev, 8081)** | Spring Boot sobre `TennisPlatformApplication`, perfil `dev`, con todas las variables puestas (incluido el bootstrap del profesor). Levanta la infraestructura antes de arrancar. |
| **Infra: postgres + mailpit** | Solo la base y el buzón, para desarrollar desde el IDE. |
| **Stack completo (compose)** | Los tres servicios con `--build`. |
| **Backend: mvn verify** | Lo mismo que ejecuta CI. |

Dos avisos:

- Para usar *Backend (dev, 8081)* hay que parar antes el contenedor `tennis-backend`, que ocupa el
  puerto 8081: `docker compose stop backend` desde `TennisPlatformApp`.
- Si IntelliJ marca en rojo el módulo de la configuración de Spring Boot, selecciona `backend` en
  el desplegable: depende de cómo esté importado el proyecto Maven.

## Decisión pendiente

Seguir con `fase-6-student` + módulo `platform`, o fusionar antes el PR #14.
