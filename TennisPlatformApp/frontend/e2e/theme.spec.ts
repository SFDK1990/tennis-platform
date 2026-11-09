import { expect, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

/**
 * The app's dark theme (47-fase32-analisis-modo-oscuro.md): the server renders the choice on
 * <html>, the system decides without one, and the profile and the website share the choice.
 */

const DARK_GROUND = "rgb(10, 23, 48)";
const LIGHT_GROUND = "rgb(238, 242, 247)";

function groundOf(page: import("@playwright/test").Page): Promise<string> {
  return page.evaluate(() => getComputedStyle(document.body).backgroundColor);
}

test("el servidor pinta el tema elegido antes de que corra ningún script", async ({ request }) => {
  const chosen = await request.get("/login", { headers: { Cookie: "theme=dark" } });
  expect(await chosen.text()).toMatch(/<html[^>]*data-theme="dark"/);

  const none = await request.get("/login");
  expect(await none.text()).not.toMatch(/<html[^>]*data-theme=/);
});

test.describe("con el sistema en oscuro", () => {
  test.use({ colorScheme: "dark" });

  test("sin elección la app sigue al sistema", async ({ page }) => {
    await page.goto("/login");
    await expect(page.locator("html")).not.toHaveAttribute("data-theme");
    expect(await groundOf(page)).toBe(DARK_GROUND);
  });
});

test("el perfil fija el tema, lo recuerda y vale también para la web", async ({ page, context }) => {
  await signIn(page, TEACHER.email, TEACHER.password);
  await page.goto("/profile");
  const html = page.locator("html");
  const appearance = page.getByRole("region", { name: "Apariencia" });
  await expect(appearance.getByRole("radio", { name: "Como el móvil" })).toBeChecked();
  expect(await groundOf(page)).toBe(LIGHT_GROUND);

  await appearance.getByRole("radio", { name: "Oscuro" }).check();
  await expect(html).toHaveAttribute("data-theme", "dark");
  expect(await groundOf(page)).toBe(DARK_GROUND);

  await page.reload();
  await expect(html).toHaveAttribute("data-theme", "dark");
  await expect(appearance.getByRole("radio", { name: "Oscuro" })).toBeChecked();

  // The website reads the same cookie, and its button changes the app's choice.
  await page.goto("/");
  await expect(html).toHaveAttribute("data-theme", "dark");
  await page.getByRole("button", { name: "Cambiar a modo claro" }).click();
  await expect(html).toHaveAttribute("data-theme", "light");
  await page.goto("/profile");
  await expect(appearance.getByRole("radio", { name: "Claro" })).toBeChecked();

  await appearance.getByRole("radio", { name: "Como el móvil" }).check();
  await expect(html).not.toHaveAttribute("data-theme");
  expect((await context.cookies()).find((cookie) => cookie.name === "theme")).toBeUndefined();
});
