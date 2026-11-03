import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import { describe, expect, it } from "vitest";

/**
 * public/sw.js is served as it is, not bundled, so the test runs the file itself with a stand-in
 * for `self` and asks its `cacheable`. Nothing personal may ever be kept on the device
 * (45-fase30-analisis-pwa-accesibilidad.md): /api and the app's pages never are.
 */
const source = readFileSync(new URL("../../public/sw.js", import.meta.url), "utf8");
const worker: { cacheable: (pathname: string) => boolean } = {} as never;
runInNewContext(source, Object.assign(worker, { self: { addEventListener: () => {}, location: { origin: "" } } }));

describe("the service worker", () => {
  it.each(["/api/v1/me", "/api/v1/bookings", "/api/v1/auth/refresh", "/home", "/bookings", "/profile", "/teacher", "/", "/login"])(
    "never keeps %s",
    (pathname) => {
      expect(worker.cacheable(pathname)).toBe(false);
    },
  );

  it.each(["/_next/static/chunks/app.js", "/_next/static/media/barlow.woff2", "/icons/icon-192.png", "/offline.html", "/offline.css", "/icon.svg"])(
    "keeps %s, which is the same for everybody",
    (pathname) => {
      expect(worker.cacheable(pathname)).toBe(true);
    },
  );
});
