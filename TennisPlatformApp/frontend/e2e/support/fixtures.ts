import { test as base, expect, type Locator, type Page } from "@playwright/test";
import { formatDay, todayIn, weekOf, type LocalDate } from "@/shared/time";
import { Arrange } from "./arrange";
import { ZONE } from "./stack";

export const test = base.extend<{ arrange: Arrange }>({
  // Not called `use`, as Playwright's examples do: the React lint would take it for the hook.
  arrange: async ({}, provide) => {
    const arrange = new Arrange();
    await provide(arrange);
    await arrange.cleanUp();
  },
});

export { expect };

export async function signIn(page: Page, email: string, password: string): Promise<void> {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Contraseña").fill(password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).not.toHaveURL(/\/login/);
}

/** Moves the calendar on screen to the week of `date` and returns that day's block. */
export async function dayInCalendar(page: Page, date: LocalDate): Promise<Locator> {
  const weeks = Math.round(
    (Date.parse(weekOf(date).from) - Date.parse(weekOf(todayIn(ZONE)).from)) / (7 * 24 * 3600 * 1000),
  );
  const day = page.getByRole("listitem").filter({ has: page.getByRole("heading", { name: formatDay(date) }) });
  for (let i = 0; i < weeks; i++) {
    await page.getByRole("button", { name: "Semana siguiente" }).click();
  }
  await expect(day).toBeVisible();
  return day;
}
