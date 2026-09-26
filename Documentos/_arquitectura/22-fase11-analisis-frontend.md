# Fase 11 — Análisis: el frontend, primer corte

Daniel quiere empezar a probar la aplicación en el navegador. Este primer corte cubre los dos
roles con el flujo mínimo; a partir de aquí, cada fase trae su pantalla. Los principios generales
están en `02-arquitectura.md` §17; aquí van sólo las decisiones de este corte.

## Alcance

El recorrido del criterio de salida de la fase, en `TennisPlatformApp/frontend`:

- **Público**: registro, login, verificación de email (el enlace que llega a Mailpit), olvido y
  restablecimiento de contraseña.
- **Los dos roles**: mi perfil (`GET`/`PATCH /me`) y cerrar sesión.
- **Alumno**: calendario con las clases que puede reservar, reservar, cancelar y mis reservas.
- **Profesor**: disponibilidad semanal y excepciones; alumnos (buscar por email, gestionar, dejar
  de gestionar); calendario con crear y cancelar clases; en cada clase, sus reservas y la
  asistencia.

Fuera: administración (Fase 12), PWA y diseño cuidado (Fase 17), E2E (Fase 14).

## Decisiones

1. **Next.js (App Router) con TypeScript estricto y Tailwind**, sin librería de componentes. Se
   valoró Vite, más ligero para una aplicación toda detrás del login, y se descartó: puede haber
   una web pública el día de mañana, y ahí Next sí aporta renderizado en servidor y SEO. Las
   páginas de la aplicación son de cliente, porque los datos cambian con cada acción y el
   renderizado en servidor complicaría la sesión sin aportar nada.
2. **Mismo origen.** Next reenvía `/api/*` al backend (`BACKEND_URL`, por defecto
   `http://localhost:8081`). Así las cookies del refresh (`path=/api/v1/auth`, `SameSite=Strict`)
   y del CSRF funcionan sin CORS.
3. **El access token vive sólo en memoria**, nunca en `localStorage`, donde un XSS lo leería. Al
   cargar la página, la sesión se recupera con `POST /auth/refresh`, que se apoya en la cookie
   `HttpOnly`, enviando `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN`.
4. **Un único cliente HTTP** en `shared/api`, sobre `openapi-fetch` con los tipos generados de
   `openapi.yaml` (`openapi-typescript`): la ruta, los parámetros y la respuesta de cada llamada
   se comprueban al compilar, y el contrato es la única fuente. El cliente añade el token, ante
   un `401` renueva una sola vez y repite, y convierte cualquier error en
   `ApiError(status, code, detail)`. Ningún componente usa `fetch`. Si el contrato no coincide
   con lo que responde el backend, se corrige en esta fase, que es cuando se nota.
5. **TanStack Query** guarda lo leído y sabe qué releer: al reservar se invalidan el calendario y
   las reservas, y un `409` invalida lo que había en pantalla. Sin ella, cada pantalla escribe a
   mano su carga, su error y su recarga.
6. **Un `409` relee**: se muestra el mensaje y se invalida lo que había en pantalla.
   Los `422` y `400` se muestran junto a la acción o el campo.
7. **Las fechas se muestran en la zona del profesor**, que llega en el calendario. Con un
   profesor en Madrid y alumnos en Madrid es lo mismo que la hora local, y evita que una clase
   "cambie de hora" si alguien abre la aplicación de viaje. Se indica la zona en la cabecera del
   calendario.
8. **El calendario es una lista por semanas**, con navegación semana a semana, no una rejilla:
   es lo mínimo que se puede usar en un móvil, y la rejilla es trabajo de la Fase 17.
9. **Rutas en inglés, textos en español.** `/verify-email` y `/reset-password` las fija el
   backend en los correos; las demás siguen el mismo criterio (`/login`, `/register`,
   `/calendar`, `/bookings`, `/teacher/...`). La zona de cada rol la protege un layout que
   redirige si el rol no corresponde; la que vale es la del backend.
10. **El código se organiza como el backend**, un paquete por módulo, para que front y back sean
    simétricos:

    ```
    src/
      app/                 rutas y layouts por rol; finas, sin lógica
      modules/
        identity/  student/  teacher/  availability/
        lesson/  booking/  calendar/
          api.ts           llamadas y hooks de TanStack Query
          components/
      shared/
        api/               cliente, sesión, ApiError, tipos generados
        ui/                piezas base: Button, Field, Dialog…
    ```

    Una regla de ESLint (`no-restricted-imports`) prohíbe que un módulo importe de otro: lo que
    dos necesitan sube a `shared`, igual que en el backend lo vigila ArchUnit. Las páginas de
    `app/` sí pueden componer varios módulos, como el *composition root*.
11. **Sin librería de formularios por ahora.** Formularios nativos, con los errores del backend
    junto a su campo. Añadir `react-hook-form` después es barato y se hace formulario a
    formulario, cuando uno lo pida.
12. **Los huecos del backend se arreglan cuando aparecen**, en esta misma fase y con su test.
    El primero conocido: no se puede reenviar el correo de verificación.
13. **Tests en esta fase**: Vitest sobre el cliente HTTP (renovar y repetir ante un `401`,
    normalizar Problem Details). Las pantallas se prueban a mano en esta fase y con Playwright
    en la 14.
14. **CI gana un job `frontend`**: `npm ci`, lint, comprobación de tipos, tests y `next build`.

## Cómo se arranca en local

```
cd TennisPlatformApp
docker compose up -d --build      # backend en :8081, Mailpit en :8025
cd frontend && npm install && npm run dev   # http://localhost:3000
```

El profesor lo crea el bootstrap con `TEACHER_EMAIL` y `TEACHER_PASSWORD` de `.env`.

## Riesgo a comprobar al implementar

La cookie del refresh sale con `Secure` por defecto. Chrome y Firefox la aceptan en
`http://localhost` porque lo tratan como contexto seguro; si algún navegador no lo hiciera, se
bajaría `cookie-secure` sólo en el perfil de desarrollo, nunca por defecto.

## Criterios de aceptación

- Daniel recorre en el navegador, con los dos roles: registro → verificación → perfil → el
  profesor lo gestiona → abre disponibilidad y crea una clase → el alumno la ve y reserva → la
  cancela o la vuelve a reservar → el profesor marca la asistencia.
- Recargar la página no cierra la sesión; cerrar sesión sí, y el refresh deja de funcionar.
- Existe un test que demuestra que un `401` renueva el token una sola vez y repite la petición.
- Existe un test que demuestra que un Problem Details llega a la pantalla como `ApiError` con su
  `code`.
- Una importación de un módulo a otro rompe el lint (se comprueba rompiéndolo).
- Un alumno con el enlace caducado puede pedir otro correo de verificación.
- El job `frontend` de CI está en verde.
