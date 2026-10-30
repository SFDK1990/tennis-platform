# Tennis Platform — Diagrama ER y borrador de DDL

Formaliza en un diagrama y en SQL las entidades ya descritas de forma textual en `03-modelo-de-datos.md`, incorporando las decisiones cerradas en `00-indice-arquitectura.md` (pista única, asociación por email, bootstrap del profesor, refresh token en cookie, cancelación libre del `ADMIN`). Es un borrador para convertir en changesets reales de Liquibase durante la Fase 0/1 del roadmap (`12-metodologia-trabajo.md`), no el changelog definitivo.

## Extensiones necesarias

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS btree_gist; -- EXCLUDE USING gist con columnas de igualdad + rango
```

## Diagrama ER

```mermaid
erDiagram
    USERS ||--o| TEACHER_PROFILES : "1..0..1"
    USERS ||--o| STUDENT_PROFILES : "1..0..1"
    USERS ||--o{ EMAIL_VERIFICATIONS : solicita
    USERS ||--o{ PASSWORD_RESET_TOKENS : solicita
    USERS ||--o{ REFRESH_TOKENS : posee
    TEACHER_PROFILES ||--o{ TEACHER_STUDENTS : gestiona
    STUDENT_PROFILES ||--o{ TEACHER_STUDENTS : "es gestionado"
    TEACHER_PROFILES ||--o{ WEEKLY_AVAILABILITY_RULES : define
    TEACHER_PROFILES ||--o{ AVAILABILITY_EXCEPTIONS : define
    TEACHER_PROFILES ||--o{ LESSONS : imparte
    LESSONS ||--o{ BOOKINGS : recibe
    STUDENT_PROFILES ||--o{ BOOKINGS : reserva
    LESSONS ||--o{ WAITING_LIST_ENTRIES : "lista de espera"
    TEACHER_PROFILES ||--o{ LEVELS : define
    LEVELS |o--o{ LESSONS : "es para"
    LEVELS |o--o{ TEACHER_STUDENTS : "nivel del alumno"
    STUDENT_PROFILES ||--o{ WAITING_LIST_ENTRIES : espera
    TEACHER_PROFILES ||--o{ MESSAGES : escribe
    MESSAGES ||--o{ MESSAGE_RECIPIENTS : "va a"
    USERS ||--o{ MESSAGE_RECIPIENTS : recibe
    USERS ||--o| STUDENT_PHOTOS : "foto del alumno"
    COURTS ||--o{ LESSONS : "se da en"

    USERS {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar role
        varchar status
        timestamptz email_verified_at
        timestamptz created_at
    }
    TEACHER_PROFILES {
        uuid user_id PK_FK
        varchar display_name
        varchar timezone
    }
    STUDENT_PROFILES {
        uuid user_id PK_FK
        varchar full_name
        varchar national_id
        varchar address
    }
    TEACHER_STUDENTS {
        uuid id PK
        uuid teacher_user_id FK
        uuid student_user_id FK
        varchar status
        timestamptz managed_at
    }
    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        varchar token_hash UK
        uuid family_id
        timestamptz expires_at
        timestamptz revoked_at
    }
    WEEKLY_AVAILABILITY_RULES {
        uuid id PK
        uuid teacher_user_id FK
        smallint day_of_week
        time start_time
        time end_time
    }
    AVAILABILITY_EXCEPTIONS {
        uuid id PK
        uuid teacher_user_id FK
        date date
        varchar type
    }
    LESSONS {
        uuid id PK
        uuid teacher_user_id FK
        varchar type
        timestamptz starts_at
        timestamptz ends_at
        smallint capacity
        varchar status
        int version
        uuid series_id
        uuid level_id
    }
    LEVELS {
        uuid id PK
        uuid teacher_user_id FK
        varchar name
        int position
    }
    BOOKINGS {
        uuid id PK
        uuid lesson_id FK
        uuid teacher_user_id FK
        uuid student_user_id FK
        varchar status
        varchar attendance
        timestamptz lesson_starts_at
        timestamptz lesson_ends_at
    }
    WAITING_LIST_ENTRIES {
        uuid id PK
        uuid lesson_id FK
        uuid teacher_user_id FK
        uuid student_user_id FK
        timestamptz joined_at
    }
```

No aparece una entidad "pista/cancha": el MVP asume una única pista (ver `01-analisis-funcional.md`), así que el solapamiento se controla solo por `teacher_user_id`.

## Changelog 1 — identity

```sql
CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email              VARCHAR(255) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    role               VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','TEACHER','STUDENT')),
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING_VERIFICATION'
                       CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','DISABLED')),
    email_verified_at  TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Email único, normalizado en minúsculas por la aplicación antes de insertar.
CREATE UNIQUE INDEX ux_users_email ON users (email);

-- Invariante de negocio "MVP: un único profesor" forzada en base de datos:
-- solo puede existir una fila con role = 'TEACHER'.
CREATE UNIQUE INDEX ux_users_single_teacher ON users ((role)) WHERE role = 'TEACHER';

CREATE INDEX ix_users_status ON users (status);

CREATE TABLE email_verifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_email_verifications_token_hash ON email_verifications (token_hash);
CREATE INDEX ix_email_verifications_expires_at ON email_verifications (expires_at);

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_password_reset_tokens_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX ix_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);

-- Necesaria por la decisión de refresh token rotatorio en cookie HttpOnly (ver 02-arquitectura.md).
CREATE TABLE refresh_tokens (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id),
    token_hash           VARCHAR(255) NOT NULL,
    family_id            UUID NOT NULL,
    issued_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at           TIMESTAMPTZ NOT NULL,
    revoked_at           TIMESTAMPTZ,
    replaced_by_token_id UUID REFERENCES refresh_tokens(id)
);
CREATE UNIQUE INDEX ux_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family_id ON refresh_tokens (family_id);
```

`family_id` agrupa toda la cadena de rotación de un mismo login; si se detecta el reuso de un `refresh_token` ya reemplazado (`replaced_by_token_id` no nulo), la aplicación debe revocar toda la familia — indicio de robo de token.

## Changelog 2 — teacher

```sql
CREATE TABLE teacher_profiles (
    user_id       UUID PRIMARY KEY REFERENCES users(id),
    display_name  VARCHAR(255) NOT NULL,
    phone         VARCHAR(30),
    timezone      VARCHAR(60) NOT NULL, -- IANA tz id, p.ej. 'Europe/Madrid'
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Se rellena una única vez, en el bootstrap inicial (junto con el `users` de rol `TEACHER`), no mediante un endpoint público de alta.

## Changelog 3 — student

```sql
CREATE TABLE student_profiles (
    user_id      UUID PRIMARY KEY REFERENCES users(id),
    full_name    VARCHAR(255) NOT NULL,
    phone        VARCHAR(30),
    national_id  VARCHAR(30),
    address      VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE teacher_students (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id  UUID NOT NULL REFERENCES teacher_profiles(user_id),
    student_user_id  UUID NOT NULL REFERENCES student_profiles(user_id),
    status           VARCHAR(20) NOT NULL DEFAULT 'MANAGED' CHECK (status IN ('MANAGED','INACTIVE')),
    managed_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deactivated_at   TIMESTAMPTZ
);

-- "Una relación alumno-profesor única" (03-modelo-de-datos.md).
CREATE UNIQUE INDEX ux_teacher_students_pair ON teacher_students (teacher_user_id, student_user_id);
CREATE INDEX ix_teacher_students_student ON teacher_students (student_user_id);
```

`national_id` y `address` son datos personales de acceso restringido (ver `02-arquitectura.md`): no deben devolverse en listados generales ni aparecer en logs; solo en el detalle de perfil accedido por el propio alumno, el profesor que lo gestiona o un `ADMIN`.

## Changelog 4 — availability

```sql
CREATE TABLE weekly_availability_rules (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id  UUID NOT NULL REFERENCES teacher_profiles(user_id),
    day_of_week      SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    active_from      DATE,
    active_until     DATE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (start_time < end_time)
);
CREATE INDEX ix_weekly_availability_teacher ON weekly_availability_rules (teacher_user_id);

CREATE TABLE availability_exceptions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id  UUID NOT NULL REFERENCES teacher_profiles(user_id),
    date             DATE NOT NULL,
    start_time       TIME,
    end_time         TIME,
    type             VARCHAR(10) NOT NULL CHECK (type IN ('BLOCK','EXTRA')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_availability_exceptions_teacher_date ON availability_exceptions (teacher_user_id, date);
```

`start_time`/`end_time` nulos en una excepción `BLOCK` significan "todo el día bloqueado"; en `EXTRA` son obligatorios (validado en la aplicación, no en el esquema, para no acoplar la regla a NULLs). Lo que el esquema sí rechaza es **media hora suelta** —una de las dos columnas sin la otra—, porque eso no describe nada bajo ninguno de los dos tipos.

**Corrección de la Fase 7 — `day_of_week` es ISO-8601 (1 = lunes … 7 = domingo).** Este documento decía `BETWEEN 0 AND 6` y `openapi.yaml` anotaba "0 = lunes". Son tres convenciones cruzadas: `java.time.DayOfWeek` numera el lunes 1, y `EXTRACT(DOW)` de PostgreSQL numera el **domingo** 0. Equivocarse entre ellas no rompe nada — mueve el horario un día y deja un sistema que funciona y miente. Se fija ISO en la base, que es lo que devuelve `DayOfWeek.getValue()` sin conversión, y la API expone el **nombre** (`MONDAY`…`SUNDAY`), de modo que el número no sale nunca del adaptador de persistencia. Razonamiento completo en `18-fase7-analisis-availability.md`.

**No hay restricción de exclusión contra reglas semanales solapadas**, a diferencia de `lessons`. Es deliberado: el solapamiento de clases nace de una carrera que dos reservantes pueden correr a la vez, mientras que el conjunto semanal pertenece a un único profesor y llega entero en una petición, así que la aplicación lo valida antes de escribir. Además, una restricción GiST aquí tendría que abarcar también el periodo de vigencia (`active_from`/`active_until`), que no cabe en el esquema sin desnormalizar.

## Changelog 5 — lesson

```sql
CREATE TABLE lessons (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id             UUID NOT NULL REFERENCES teacher_profiles(user_id),
    type                        VARCHAR(10) NOT NULL CHECK (type IN ('INDIVIDUAL','GROUP')),
    starts_at                   TIMESTAMPTZ NOT NULL,
    ends_at                     TIMESTAMPTZ NOT NULL,
    capacity                    SMALLINT NOT NULL CHECK (capacity > 0),
    status                      VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                                CHECK (status IN ('OPEN','FULL','CANCELLED','COMPLETED')),
    notes                       TEXT,
    created_outside_availability BOOLEAN NOT NULL DEFAULT false,
    version                     INT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (starts_at < ends_at),
    CHECK (type <> 'INDIVIDUAL' OR capacity = 1),
    -- Duración múltiplo de 30 minutos.
    CHECK (MOD(EXTRACT(EPOCH FROM (ends_at - starts_at))::INT, 1800) = 0)
);

-- "No puede haber solapamiento de clases del profesor" — se excluyen las canceladas.
ALTER TABLE lessons ADD CONSTRAINT ex_lessons_no_teacher_overlap
    EXCLUDE USING gist (
        teacher_user_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status <> 'CANCELLED');

CREATE INDEX ix_lessons_teacher_starts_at ON lessons (teacher_user_id, starts_at);
CREATE INDEX ix_lessons_status ON lessons (status);
```

La regla "no cruza medianoche" depende de la zona horaria local del profesor (la columna `starts_at`/`ends_at` guarda instantes UTC), así que se valida en la capa de aplicación al convertir a hora local, no como `CHECK` de base de datos.

`version` soporta bloqueo optimista adicional a nivel de aplicación; el bloqueo pesimista real de la reserva (`SELECT ... FOR UPDATE`) sigue siendo el mecanismo principal descrito en `02-arquitectura.md` §11. La columna existe desde la Fase 8 pero **no se mapea todavía**: la Fase 9 es la que le da sentido, y arrastrar un número que nadie lee sólo invita a creer que hace algo.

**Corrección de la Fase 8 — `status` guarda dos valores, no cuatro.** El `CHECK` real es
`status IN ('OPEN','CANCELLED')`, y la tabla gana una columna `cancelled_at` ligada a él por
`CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL))`. `FULL` y `COMPLETED` se derivan
al leer. El motivo es que ninguno de los dos puede mantenerse honesto como columna: `FULL`
significa "no quedan plazas", que sólo `booking` puede contar, así que habría que escribirlo
desde allí y mantenerlo en paz con el recuento real — el día que discrepen, la clase no falla,
miente. `COMPLETED` es un hecho sobre el reloj, y derivarlo evita un proceso programado que el
MVP no tiene. La columna guarda sólo lo que decidió una persona. Razonamiento completo en
`19-fase8-analisis-lesson.md`.

Por el mismo criterio **no hay columna `cancelled_within_window`**: si la cancelación dejó menos
de 24 horas es `cancelled_at` restado de `starts_at`, y escribirlo sería una segunda copia de
una resta.

**La Fase 8 añade también `platform_configuration.max_group_capacity`** (`INT NOT NULL DEFAULT 8
CHECK (> 0)`), el tope de plazas de una clase grupal. Hasta entonces la única regla era
`capacity > 0`, así que un error de tecleo podía crear una clase de diez mil plazas sobre una
única pista. Vive en la configuración y no en el código porque el día que el número esté mal, lo
estará para todas las clases a la vez.

## Changelog 6 — booking

**Corregido en la Fase 9.** Lo que había aquí era el borrador; el changeset real es
`v7-booking.yaml` (la numeración de este documento no coincide con la de los ficheros).
Razonamiento completo en `20-fase9-analisis-booking.md`.

```sql
CREATE TABLE bookings (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id         UUID NOT NULL REFERENCES lessons(id),
    teacher_user_id   UUID NOT NULL REFERENCES teacher_profiles(user_id),
    student_user_id   UUID NOT NULL REFERENCES student_profiles(user_id),
    status            VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN (
                          'CONFIRMED','CANCELLED_BY_STUDENT','CANCELLED_BY_TEACHER',
                          'CANCELLED_BY_ADMIN')),
    attendance        VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                      CHECK (attendance IN ('PENDING','ATTENDED','NO_SHOW')),
    -- Copias inmutables de la clase: la EXCLUDE no puede hacer JOIN contra lessons.
    lesson_starts_at  TIMESTAMPTZ NOT NULL,
    lesson_ends_at    TIMESTAMPTZ NOT NULL,
    booked_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_bookings_period CHECK (lesson_starts_at < lesson_ends_at),
    CONSTRAINT ck_bookings_cancelled_at CHECK ((status = 'CONFIRMED') = (cancelled_at IS NULL))
);

CREATE UNIQUE INDEX ux_bookings_active_pair
    ON bookings (lesson_id, student_user_id) WHERE status = 'CONFIRMED';

ALTER TABLE bookings ADD CONSTRAINT ex_bookings_no_student_overlap
    EXCLUDE USING gist (
        student_user_id WITH =,
        tstzrange(lesson_starts_at, lesson_ends_at) WITH &&
    ) WHERE (status = 'CONFIRMED');

CREATE INDEX ix_bookings_lesson_status    ON bookings (lesson_id, status);
CREATE INDEX ix_bookings_student_starts_at ON bookings (student_user_id, lesson_starts_at);
CREATE INDEX ix_bookings_teacher_starts_at ON bookings (teacher_user_id, lesson_starts_at);
```

Tres cambios respecto al borrador, los tres deliberados:

- **La asistencia sale de `status` a su propia columna.** El borrador decía que `ATTENDED` y
  `NO_SHOW` eran valores de `status` porque "la asistencia no tiene ciclo de vida propio". El
  problema no era el ciclo de vida sino los filtros: el índice único, la restricción de exclusión
  y el recuento de plazas miran `status = 'CONFIRMED'`, y marcar a un alumno como asistido lo
  habría sacado de los tres a la vez. Que la reserva siga en pie y que el alumno viniera son dos
  hechos, y cada uno tiene su columna.
- **`teacher_user_id` se copia en la reserva**, igual que los instantes: es el filtro de todas
  las consultas del lado del profesor, y una clase nunca cambia de profesor.
- **No hay trigger de capacidad.** El único camino que añade una reserva confirmada bloquea antes
  la fila de la clase (`SELECT ... FOR UPDATE`), y `LastSeatConcurrencyTest` lo demuestra —falla
  si se quita el cerrojo, y se comprobó quitándolo—. Un trigger repetiría la regla en PL/pgSQL, y
  de dos copias la que nadie lee es la que se desincroniza.

## Changelog 5b — series semanales (Fase 22)

`v10-lesson-series.yaml`. Razonamiento en `35-fase22-analisis-clases-que-se-repiten.md`.

```sql
ALTER TABLE lessons ADD COLUMN series_id UUID;
CREATE INDEX ix_lessons_series ON lessons (series_id, starts_at) WHERE series_id IS NOT NULL;
```

## Changelog 5c — niveles y ficha del alumno (Fase 23)

`v11-levels.yaml`. Razonamiento en `36-fase23-analisis-niveles-ficha.md`. Crea los cuatro niveles
iniciales del profesor que ya existe.

```sql
CREATE TABLE levels (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id  UUID NOT NULL REFERENCES teacher_profiles(user_id),
    name             VARCHAR(60) NOT NULL,
    position         INT NOT NULL,
    CONSTRAINT ux_levels_teacher_name UNIQUE (teacher_user_id, name)
);
ALTER TABLE teacher_students
    ADD COLUMN level_id UUID REFERENCES levels(id) ON DELETE RESTRICT,
    ADD COLUMN goals VARCHAR(2000),
    ADD COLUMN private_notes VARCHAR(2000);
ALTER TABLE lessons ADD COLUMN level_id UUID REFERENCES levels(id) ON DELETE RESTRICT;
```

## Changelog 5d — mensajes (Fase 24)

`v12-notification-messages.yaml`. Razonamiento en `37-fase24-analisis-mensajes.md`.

```sql
CREATE TABLE messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id  UUID NOT NULL REFERENCES teacher_profiles(user_id),
    audience         VARCHAR(10) NOT NULL CHECK (audience IN ('ALL', 'LEVEL', 'LESSON', 'SERIES')),
    audience_label   VARCHAR(120) NOT NULL,
    subject          VARCHAR(120) NOT NULL,
    body             VARCHAR(4000) NOT NULL,
    sent_at          TIMESTAMPTZ NOT NULL,
    recipient_count  INT NOT NULL CHECK (recipient_count > 0)
);
CREATE INDEX ix_messages_teacher_sent_at ON messages (teacher_user_id, sent_at DESC);
CREATE TABLE message_recipients (
    message_id       UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    student_user_id  UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (message_id, student_user_id)
);
CREATE INDEX ix_message_recipients_student ON message_recipients (student_user_id);
```

## Changelog 3b — foto del alumno (Fase 25.5)

`v13-student-photos.yaml`. Razonamiento en `39-fase25.5-analisis-foto-alumno.md`.

```sql
CREATE TABLE student_photos (
    user_id       UUID PRIMARY KEY REFERENCES users(id),
    content_type  VARCHAR(20) NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    data          BYTEA NOT NULL CHECK (octet_length(data) <= 1048576),
    updated_at    TIMESTAMPTZ NOT NULL
);
```

## Changelog 5e — pistas (Fase 25.6)

`v14-lesson-courts.yaml`. Razonamiento en `40-fase25.6-analisis-pistas.md`.

```sql
CREATE TABLE courts (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(60) NOT NULL UNIQUE,
    position  INT NOT NULL UNIQUE
);
INSERT INTO courts (name, position) VALUES ('Pista 1', 1), ('Pista 2', 2);
ALTER TABLE lessons ADD COLUMN court_id UUID REFERENCES courts(id);
UPDATE lessons SET court_id = (SELECT id FROM courts WHERE position = 1);
ALTER TABLE lessons ALTER COLUMN court_id SET NOT NULL;
```

## Changelog 6c — bonos (Fase 27)

`v15-passes.yaml`. Razonamiento en `41-fase27-analisis-bonos.md`.

```sql
CREATE TABLE pass_types (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id  UUID NOT NULL REFERENCES teacher_profiles(user_id),
    name             VARCHAR(60) NOT NULL,
    lesson_type      VARCHAR(20) NOT NULL CHECK (lesson_type IN ('INDIVIDUAL', 'GROUP')),
    lessons          INT NOT NULL CHECK (lessons BETWEEN 1 AND 200),
    price_cents      INT NOT NULL CHECK (price_cents >= 0),
    validity_months  INT CHECK (validity_months BETWEEN 1 AND 36),
    retired          BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (teacher_user_id, name)
);
CREATE TABLE passes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id  UUID NOT NULL REFERENCES teacher_profiles(user_id),
    student_user_id  UUID NOT NULL REFERENCES student_profiles(user_id),
    pass_type_id     UUID NOT NULL REFERENCES pass_types(id) ON DELETE RESTRICT,
    name             VARCHAR(60) NOT NULL,
    lesson_type      VARCHAR(20) NOT NULL CHECK (lesson_type IN ('INDIVIDUAL', 'GROUP')),
    lessons          INT NOT NULL CHECK (lessons BETWEEN 1 AND 200),
    price_cents      INT NOT NULL CHECK (price_cents >= 0),
    paid_on          DATE NOT NULL,
    payment_method   VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'BIZUM', 'TRANSFER')),
    valid_until      DATE,
    created_at       TIMESTAMPTZ NOT NULL,
    voided_at        TIMESTAMPTZ
);
CREATE INDEX ix_passes_student ON passes (student_user_id, lesson_type);
CREATE TABLE pass_settings (
    teacher_user_id  UUID PRIMARY KEY REFERENCES teacher_profiles(user_id),
    required         BOOLEAN NOT NULL
);
ALTER TABLE bookings ADD COLUMN pass_id UUID REFERENCES passes(id);
CREATE INDEX ix_bookings_pass ON bookings (pass_id) WHERE pass_id IS NOT NULL;
```

## Changelog 6d — compras de bonos en la app (Fase 27.5)

`v16-pass-purchases.yaml`. Razonamiento en `42-fase27.5-analisis-pago-online.md`.

```sql
ALTER TABLE pass_settings ADD COLUMN online_payment BOOLEAN NOT NULL DEFAULT FALSE;
-- passes.payment_method admite además 'ONLINE', que sólo pone la app.
CREATE TABLE pass_purchases (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id      UUID NOT NULL REFERENCES teacher_profiles(user_id),
    student_user_id      UUID NOT NULL REFERENCES student_profiles(user_id),
    pass_type_id         UUID NOT NULL REFERENCES pass_types(id) ON DELETE RESTRICT,
    name                 VARCHAR(60) NOT NULL,
    lesson_type          VARCHAR(20) NOT NULL CHECK (lesson_type IN ('INDIVIDUAL', 'GROUP')),
    lessons              INT NOT NULL CHECK (lessons BETWEEN 1 AND 200),
    price_cents          INT NOT NULL CHECK (price_cents >= 50),
    validity_months      INT CHECK (validity_months BETWEEN 1 AND 36),
    status               VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'EXPIRED')),
    checkout_session_id  VARCHAR(255) UNIQUE,
    checkout_url         VARCHAR(1024),
    pass_id              UUID UNIQUE REFERENCES passes(id),
    created_at           TIMESTAMPTZ NOT NULL,
    expires_at           TIMESTAMPTZ NOT NULL,
    paid_at              TIMESTAMPTZ,
    CHECK ((status = 'PAID') = (pass_id IS NOT NULL))
);
CREATE INDEX ix_pass_purchases_student ON pass_purchases (student_user_id);
```

## Changelog 6b — lista de espera (Fase 21)

`v9-booking-waiting-list.yaml`. Razonamiento en `34-fase21-analisis-lista-espera.md`.

```sql
CREATE TABLE waiting_list_entries (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id        UUID NOT NULL REFERENCES lessons(id),
    teacher_user_id  UUID NOT NULL REFERENCES teacher_profiles(user_id),
    student_user_id  UUID NOT NULL REFERENCES student_profiles(user_id),
    joined_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_waiting_list_pair UNIQUE (lesson_id, student_user_id)
);
CREATE INDEX ix_waiting_list_lesson_order ON waiting_list_entries (lesson_id, joined_at, id);
CREATE INDEX ix_waiting_list_student ON waiting_list_entries (student_user_id);
```

## Changelog 7 — platform (configuración global)

> **Corregido en la Fase 6.** Esta tabla estaba agrupada aquí como "administration" porque la
> consola de administración es lo que la edita. El dueño de una tabla es el módulo dueño del
> dato: `student` tiene que leer `student_limit` para validar el límite al asociar un alumno, y
> `student` no puede depender de `administration`. La tabla pasa al módulo `platform`, que no
> depende de nadie, y su changeset se ejecuta como `v4-platform` —antes de lo que sugiere el
> número 7 de este documento— precisamente porque `student` ya la necesita.
>
> `updated_by` sigue siendo clave ajena a `users`: en código `platform` no depende de
> `identity`, solo guarda un UUID, pero la integridad referencial de un campo de auditoría vale
> más que la pureza del diagrama.

```sql
CREATE TABLE platform_configuration (
    id             SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1), -- fila única (singleton)
    student_limit  INT NOT NULL CHECK (student_limit > 0),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by     UUID REFERENCES users(id)
);
```

El `CHECK (id = 1)` junto con la clave primaria impide insertar una segunda fila, garantizando que la configuración global sea siempre singleton. La fila se siembra en el propio changeset con `student_limit = 50`, para que la configuración exista desde el primer arranque y la Fase 7 solo tenga que actualizarla. El límite de alumnos gestionados en sí (contar `teacher_students` con `status = 'MANAGED'` y compararlo con `student_limit`) se valida en la aplicación, no en el esquema.

## Changelog 8 — índices y constraints adicionales

Cubierto ya en los changelogs anteriores; este changelog queda reservado para ajustes de rendimiento (p. ej. índices compuestos adicionales) que solo se justifiquen con datos reales de uso, siguiendo la recomendación de `03-modelo-de-datos.md` de no adelantar índices sin evidencia.
