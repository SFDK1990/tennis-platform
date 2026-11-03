/**
 * Registers public/sw.js (45-fase30-analisis-pwa-accesibilidad.md) once the page has hydrated, so
 * it never competes with the first render. Only in a production build: under `next dev` it would
 * keep files that are still changing.
 */
export function registerServiceWorker(): void {
  if (process.env.NODE_ENV !== "production" || !("serviceWorker" in navigator)) return;
  navigator.serviceWorker.register("/sw.js", { scope: "/" }).catch(() => {
    // Without the worker the app works the same, only without the offline page.
  });
}
