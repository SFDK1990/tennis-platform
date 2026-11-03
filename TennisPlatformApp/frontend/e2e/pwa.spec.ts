import { expect, signIn, test } from "./support/fixtures";
import type { Page } from "@playwright/test";

/**
 * The installable app of 45-fase30-analisis-pwa-accesibilidad.md: the manifest and its icons, the
 * offline page, and a cache that never keeps anything personal. In Chromium, whose service worker
 * Playwright drives; WebKit runs the student's flows in *.student.spec.ts.
 */

/** The width and height a PNG declares in its header. */
function pngSize(bytes: Buffer): string {
  return `${bytes.readUInt32BE(16)}x${bytes.readUInt32BE(20)}`;
}

/** Waits until the worker controls the page: only then do its rules apply to what it loads. */
async function controlledByWorker(page: Page): Promise<void> {
  await page.waitForFunction(() => navigator.serviceWorker.controller !== null);
}

test("el manifest hace instalable la app, que arranca en el inicio de cada rol", async ({ request }) => {
  const response = await request.get("/manifest.webmanifest");
  expect(response.ok()).toBe(true);
  const manifest = await response.json();
  expect(manifest).toMatchObject({ name: "Oasis Papagayo", start_url: "/home", scope: "/", display: "standalone" });

  const purposes = manifest.icons.map((icon: { purpose: string }) => icon.purpose);
  expect(purposes).toEqual(expect.arrayContaining(["any", "maskable"]));
  for (const icon of manifest.icons) {
    const file = await request.get(icon.src);
    expect(file.headers()["content-type"], icon.src).toBe("image/png");
    expect(pngSize(await file.body()), icon.src).toBe(icon.sizes);
  }
});

test("las páginas enlazan el manifest y el icono de iPhone", async ({ page, request }) => {
  await page.goto("/login");
  await expect(page.locator('link[rel="manifest"]')).toHaveAttribute("href", "/manifest.webmanifest");
  const appleIcon = await page.locator('link[rel="apple-touch-icon"]').getAttribute("href");
  const file = await request.get(appleIcon!);
  expect(pngSize(await file.body())).toBe("180x180");
});

test("sin conexión sale la página «Sin conexión», y /api no sale de la caché", async ({ page, context, arrange }) => {
  const student = await arrange.student({ managed: true });
  await signIn(page, student.email, student.password);
  await controlledByWorker(page);

  await context.setOffline(true);
  try {
    await page.goto("/bookings");
    await expect(page.getByRole("heading", { level: 1, name: "Sin conexión" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Reintentar" })).toBeVisible();
    const api = await page.evaluate(() => fetch("/api/v1/me").then((response) => response.status, () => "sin red"));
    expect(api).toBe("sin red");
  } finally {
    await context.setOffline(false);
  }
  await page.getByRole("link", { name: "Reintentar" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Mis reservas" })).toBeVisible();
});

test("tras recorrer la app con sesión, la caché sólo guarda lo que es igual para todos", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  const day = await arrange.freeDay();
  await arrange.book(student, await arrange.lesson({ date: day, time: "10:00", capacity: 2 }));

  await signIn(page, student.email, student.password);
  await controlledByWorker(page);
  for (const path of ["/home", "/calendar", "/bookings", "/passes", "/messages", "/profile"]) {
    await page.goto(path);
    await expect(page.getByText("Cargando…")).toHaveCount(0);
  }

  const kept: string[] = await page.evaluate(async () => {
    const paths: string[] = [];
    for (const name of await caches.keys()) {
      for (const request of await (await caches.open(name)).keys()) paths.push(new URL(request.url).pathname);
    }
    return paths;
  });
  expect(kept.length).toBeGreaterThan(0);
  const personal = kept.filter((path) => !/^\/(_next\/static\/|icons\/|offline\.(html|css)$|icon\.svg$)/.test(path));
  expect(personal).toEqual([]);
});
