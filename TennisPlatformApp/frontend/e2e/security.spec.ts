import { expect, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

test("las páginas llevan las cabeceras de seguridad y la CSP no bloquea nada de la aplicación", async ({ page }) => {
  const violations: string[] = [];
  page.on("console", (message) => {
    if (message.text().includes("Content Security Policy")) violations.push(message.text());
  });

  const response = await page.goto("/login");
  const headers = response!.headers();
  expect(headers["content-security-policy"]).toMatch(/script-src 'self' 'nonce-[^']+' 'strict-dynamic'/);
  expect(headers["content-security-policy"]).toContain("frame-ancestors 'none'");
  expect(headers["referrer-policy"]).toBe("no-referrer");
  expect(headers["x-frame-options"]).toBe("DENY");
  expect(headers["x-powered-by"]).toBeUndefined();

  await signIn(page, TEACHER.email, TEACHER.password);
  await expect(page.getByRole("heading", { name: "Hoy", exact: true })).toBeVisible();
  expect(violations).toEqual([]);
});

test("cada petición recibe un nonce distinto", async ({ request }) => {
  const nonceOf = async () =>
    (await request.get("/login")).headers()["content-security-policy"].match(/'nonce-([^']+)'/)![1];

  expect(await nonceOf()).not.toBe(await nonceOf());
});
