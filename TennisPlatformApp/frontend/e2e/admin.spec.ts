import { expect, signIn, test } from "./support/fixtures";
import { ADMIN, TEACHER } from "./support/stack";

test("con el límite de alumnos alcanzado, el profesor no puede añadir otro", async ({ page, arrange }) => {
  await arrange.keepConfiguration();
  const student = await arrange.student();
  const managed = await arrange.managedCount();

  await signIn(page, ADMIN.email, ADMIN.password);
  await page.getByLabel("Límite de alumnos").fill(String(Math.max(managed, 1)));
  await page.getByRole("button", { name: "Guardar configuración" }).click();
  await expect(page.getByText("Configuración guardada.")).toBeVisible();
  await page.reload();
  await expect(page.getByLabel("Límite de alumnos")).toHaveValue(String(Math.max(managed, 1)));

  if (managed === 0) {
    // A limit cannot be zero, so with nobody managed yet the limit is filled with one student.
    await arrange.manage(await arrange.student());
  }
  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, TEACHER.email, TEACHER.password);
  await page.getByRole("link", { name: "Alumnos" }).click();
  await page.getByLabel("Email del alumno").fill(student.email);
  await page.getByRole("button", { name: "Buscar" }).click();
  await page.getByRole("button", { name: "Añadir a mis alumnos" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Has llegado al límite de alumnos." })).toBeVisible();
});

test("un alumno desactivado por el admin ya no puede entrar", async ({ page, arrange }) => {
  const student = await arrange.student();

  await signIn(page, ADMIN.email, ADMIN.password);
  await page.getByRole("link", { name: "Usuarios" }).click();
  await page.getByLabel("Email").fill(student.email);
  await page.getByRole("button", { name: "Buscar" }).click();
  const row = page.getByRole("listitem").filter({ hasText: student.email });
  await expect(row).toContainText("Alumno, activa");
  await row.getByRole("button", { name: "Desactivar" }).click();
  await row.getByRole("button", { name: "Sí, desactivar" }).click();
  await expect(row).toContainText("Alumno, desactivada");

  await page.getByRole("button", { name: "Salir" }).click();
  await expect(page).toHaveURL(/\/login/);
  await page.getByLabel("Email").fill(student.email);
  await page.getByLabel("Contraseña").fill(student.password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "El email o la contraseña no son correctos." })).toBeVisible();
});
