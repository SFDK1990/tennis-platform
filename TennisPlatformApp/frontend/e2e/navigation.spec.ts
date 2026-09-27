import type { Page } from "@playwright/test";
import { expect, openFromMenu, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

// The narrowest phone the design is drawn for (27-fase15.5, decision 3).
test.use({ viewport: { width: 390, height: 844 } });

/** On a phone there is one navigation, the tab bar at the bottom; the sidebar is not shown. */
async function expectTabBar(page: Page, items: string[]): Promise<void> {
  const nav = page.getByRole("navigation");
  await expect(nav).toHaveCount(1);
  const box = (await nav.boundingBox())!;
  expect(box.y + box.height).toBeGreaterThan(844 - 2);
  await expect(nav.getByRole("link")).toHaveText(items);
  for (const item of items.toReversed()) {
    await openFromMenu(page, item);
  }
}

test("en el móvil, el alumno se mueve con la barra de abajo", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  await signIn(page, student.email, student.password);
  await expectTabBar(page, ["Inicio", "Clases", "Mis reservas", "Perfil"]);
});

test("en el móvil, el profesor se mueve con la barra de abajo y sale desde su perfil", async ({ page }) => {
  await signIn(page, TEACHER.email, TEACHER.password);
  await expectTabBar(page, ["Hoy", "Agenda", "Alumnos", "Horario", "Perfil"]);
  await openFromMenu(page, "Perfil");
  await page.getByRole("button", { name: "Salir" }).click();
  await expect(page).toHaveURL(/\/login/);
});
