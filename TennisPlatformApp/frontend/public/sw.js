/*
 * The app's service worker (45-fase30-analisis-pwa-accesibilidad.md). It keeps only what is the
 * same for everybody: Next's hashed static files, the icons and the offline page. Pages and /api
 * always go to the network, so nothing personal is ever stored on the device, and nothing is
 * booked or cancelled without a connection (02-arquitectura.md §13).
 *
 * Bump VERSION when offline.html, offline.css or the icons change: the new worker then fills a
 * new cache and deletes the old one. /_next/static needs no bump, its file names carry a hash.
 */
const VERSION = "1";
const CACHE = `oasis-static-${VERSION}`;
const OFFLINE_PAGE = "/offline.html";
const PRECACHE = [OFFLINE_PAGE, "/offline.css", "/icon.svg"];

/** Whether a same-origin GET may be answered from the cache. Nothing else ever is. */
function cacheable(pathname) {
  return (
    pathname.startsWith("/_next/static/") ||
    pathname.startsWith("/icons/") ||
    PRECACHE.includes(pathname)
  );
}

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(PRECACHE)));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  // A page: always the network; without it, the offline page. The page itself is never kept.
  if (request.mode === "navigate") {
    event.respondWith(fetch(request).catch(() => caches.match(OFFLINE_PAGE)));
    return;
  }

  if (!cacheable(url.pathname)) return;
  event.respondWith(
    caches.open(CACHE).then((cache) =>
      cache.match(request).then(
        (cached) =>
          cached ||
          fetch(request).then((response) => {
            if (response.ok) cache.put(request, response.clone());
            return response;
          }),
      ),
    ),
  );
});
