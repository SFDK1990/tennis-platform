import AxeBuilder from "@axe-core/playwright";
import type { Page } from "@playwright/test";
import { recommendClass, trialHref } from "@/app/(web)/_parts/school";
import { expect, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

/** The public website of 31-fase31-analisis-web-publica.md. */

const LEGAL_PAGES = ["/aviso-legal", "/privacidad"];

/** Criterion 1. */
test("sin sesión, la portada es la web de la escuela y lleva a pedir la clase de prueba", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1, name: /Clases de tenis en/ })).toBeVisible();
  await expect(page.getByRole("link", { name: "Pedir clase de prueba" }).first()).toHaveAttribute("href", trialHref());
  await expect(page.getByRole("link", { name: "Entrar", exact: true })).toBeVisible();
});

/** Decision 21: two answers suggest a class, and the trial button asks for that one. */
test("«¿Qué clase es para mí?» recomienda una clase y pide su prueba", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("radio", { name: "Para mi hijo o hija" }).check({ force: true });
  await page.getByRole("radio", { name: "En grupo" }).check({ force: true });
  await expect(page.getByRole("link", { name: "Pedir la prueba de niños" })).toHaveAttribute(
    "href",
    trialHref(undefined, recommendClass("child", "group").message),
  );

  await page.getByRole("radio", { name: "Clase particular" }).check({ force: true });
  await expect(page.getByRole("link", { name: "Pedir la prueba de particulares" })).toBeVisible();
});

/** Criterion 2: nobody is redirected away from the website; the link changes instead. */
test("con sesión, la web lleva a cada uno a su inicio", async ({ page, arrange }) => {
  const student = await arrange.student();
  await signIn(page, student.email, student.password);
  await page.goto("/");
  await page.getByRole("link", { name: "Mi cuenta" }).click();
  await expect(page).toHaveURL(/\/home$/);

  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, TEACHER.email, TEACHER.password);
  await page.goto("/");
  await page.getByRole("link", { name: "Mi cuenta" }).click();
  await expect(page).toHaveURL(/\/teacher$/);
});

test("desde el login se vuelve a la web, por el botón y por el logo", async ({ page }) => {
  await page.goto("/login");
  await page.getByRole("link", { name: "Ir a la web" }).click();
  await expect(page.getByRole("heading", { level: 1, name: /Clases de tenis en/ })).toBeVisible();

  await page.goto("/register");
  await page.getByRole("link", { name: "Oasis Papagayo Escuela de tenis" }).click();
  await expect(page.getByRole("heading", { level: 1, name: /Clases de tenis en/ })).toBeVisible();
});

/** The logo goes to the role's home, so the app needs its own way back to the website. */
test("desde la app se vuelve a la web, en el ordenador y en el móvil", async ({ page }) => {
  await signIn(page, TEACHER.email, TEACHER.password);
  await page.getByRole("link", { name: "Ir a la web" }).click();
  await expect(page.getByRole("heading", { level: 1, name: /Clases de tenis en/ })).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("link", { name: "Mi cuenta" }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await page.getByRole("link", { name: "Ir a la web" }).click();
  await expect(page.getByRole("heading", { level: 1, name: /Clases de tenis en/ })).toBeVisible();
});

test.describe("con el sistema en oscuro", () => {
  test.use({ colorScheme: "dark" });

  /** Criterion 3. */
  test("la web sigue al sistema hasta que el visitante elige, y recuerda su elección", async ({ page }) => {
    await page.goto("/");
    const site = page.locator("[data-site]");
    await expect(site).not.toHaveAttribute("data-theme");
    await expect(page.getByRole("button", { name: "Cambiar a modo claro" })).toBeVisible();

    await page.getByRole("button", { name: "Cambiar a modo claro" }).click();
    await expect(site).toHaveAttribute("data-theme", "light");

    await page.reload();
    // Rendered so by the server, from the cookie: no script had to run first.
    await expect(site).toHaveAttribute("data-theme", "light");
    await expect(page.getByRole("button", { name: "Cambiar a modo oscuro" })).toBeVisible();
  });
});

/**
 * Criterion 4: a blocked script or style fails silently, so the console is the only witness.
 * The login screen is already covered by security.spec.ts.
 */
test("ni la web ni el registro chocan con la CSP", async ({ page }) => {
  const violations: string[] = [];
  page.on("console", (message) => {
    if (message.text().includes("Content Security Policy")) {
      violations.push(message.text());
    }
  });
  await page.goto("/");
  // The theme button only works once React has hydrated, which a blocked script would prevent.
  await page.getByRole("button", { name: /Cambiar a modo/ }).click();
  await expect(page.locator("[data-site]")).toHaveAttribute("data-theme", /light|dark/);

  await page.goto("/register");
  await expect(page.getByRole("button", { name: "Crear cuenta" })).toBeVisible();
  // Criterion 11: the legal pages too.
  for (const path of LEGAL_PAGES) {
    await page.goto(path);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  }
  expect(violations).toEqual([]);
});

/** Criterion 10: the legal texts are one click away from wherever data is asked for or shown. */
test("desde la web, el login y el registro se llega al aviso legal y a la privacidad", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("contentinfo").getByRole("link", { name: "Aviso legal" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Aviso legal" })).toBeVisible();
  await page.getByRole("contentinfo").getByRole("link", { name: "Privacidad" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Privacidad" })).toBeVisible();

  await page.goto("/login");
  await page.getByRole("navigation", { name: "Legal" }).getByRole("link", { name: "Aviso legal" }).click();
  await expect(page).toHaveURL(/\/aviso-legal$/);

  await page.goto("/register");
  await page.getByRole("link", { name: "política de privacidad" }).click();
  await expect(page).toHaveURL(/\/privacidad$/);
});

/** Criterion 6. */
test("la web se deja encontrar: robots, sitemap y datos estructurados", async ({ page, request }) => {
  const robots = await request.get("/robots.txt");
  expect(robots.status()).toBe(200);
  expect(await robots.text()).toContain("Disallow: /teacher");
  expect((await request.get("/sitemap.xml")).status()).toBe(200);

  await page.goto("/");
  const data = JSON.parse((await page.locator('script[type="application/ld+json"]').textContent()) ?? "{}");
  expect(data["@type"]).toBe("SportsActivityLocation");
});

/** Criterion 5, in both themes and at both sizes. */
for (const colorScheme of ["light", "dark"] as const) {
  for (const [size, viewport] of [["escritorio", { width: 1440, height: 900 }], ["móvil", { width: 390, height: 844 }]] as const) {
    test.describe(`en ${size}, tema ${colorScheme === "light" ? "claro" : "oscuro"}`, () => {
      test.use({ colorScheme, viewport });

      /** And criterion 11 for the legal pages. */
      test("axe no encuentra fallos graves en la web", async ({ page }) => {
        for (const path of ["/", ...LEGAL_PAGES]) {
          await page.goto(path);
          expect(await seriousViolations(page), path).toEqual([]);
        }
      });
    });
  }
}

async function seriousViolations(page: Page): Promise<string[]> {
  const results = await new AxeBuilder({ page }).analyze();
  return results.violations
    .filter((violation) => violation.impact === "serious" || violation.impact === "critical")
    .map((violation) => `${violation.id}: ${violation.nodes.map((node) => node.target.join(" ")).join(", ")}`);
}
