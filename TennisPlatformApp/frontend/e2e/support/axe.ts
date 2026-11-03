import AxeBuilder from "@axe-core/playwright";
import { expect, type Page } from "@playwright/test";

/**
 * The serious and critical violations axe finds on the page as it is now, one line each: what
 * fails the criterion of 31-fase31 and 45-fase30. Moderate ones are reviewed by hand.
 */
export async function seriousViolations(page: Page): Promise<string[]> {
  const results = await new AxeBuilder({ page }).analyze();
  return results.violations
    .filter((violation) => violation.impact === "serious" || violation.impact === "critical")
    .map((violation) => `${violation.id}: ${violation.nodes.map((node) => node.target.join(" ")).join(", ")}`);
}

/**
 * Opens a screen of the app, waits until it has its heading and nothing left loading, and checks
 * it with axe: a screen still saying «Cargando…» would pass without its content being looked at.
 */
export async function expectAccessible(page: Page, path: string): Promise<void> {
  await page.goto(path);
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(page.getByText("Cargando…")).toHaveCount(0);
  expect(await seriousViolations(page), path).toEqual([]);
}
