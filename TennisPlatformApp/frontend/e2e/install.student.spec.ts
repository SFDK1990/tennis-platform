import { expect, signIn, test } from "./support/fixtures";

/**
 * 45-fase30-analisis-pwa-accesibilidad.md, decision 2: the student's profile says how to install
 * the app on their phone, with the steps of its browser. The Pixel runs it in Chromium, the iPhone
 * in WebKit.
 */
test("el perfil del alumno explica cómo instalar la app en su móvil", async ({ page, arrange }, testInfo) => {
  const student = await arrange.student();
  await signIn(page, student.email, student.password);
  await page.goto("/profile");

  const install = page.getByRole("region", { name: "Instálala en tu móvil" });
  await expect(install).toBeVisible();
  const iPhone = testInfo.project.name.includes("webkit");
  await expect(install).toContainText(iPhone ? "Añadir a pantalla de inicio" : "Instalar aplicación");
  await expect(install).toContainText(iPhone ? "Ábrela en Safari." : "Ábrela en Chrome.");
});
