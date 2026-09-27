import { linkSentTo } from "./support/mailbox";
import { expect, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

/** The access token lives only in memory: after a reload, only the refresh cookie brings it back. */
test("la sesión sobrevive a una recarga y termina al salir", async ({ page, arrange }) => {
  const student = await arrange.student();

  await signIn(page, student.email, student.password);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Clases" })).toBeVisible();

  await page.getByRole("button", { name: "Salir" }).click();
  await expect(page).toHaveURL(/\/login/);
  await page.goto("/calendar");
  await expect(page).toHaveURL(/\/login/);
});

test("sin sesión, una pantalla protegida lleva al login", async ({ page }) => {
  await page.goto("/teacher/students");
  await expect(page).toHaveURL(/\/login/);
});

test("cada rol se queda en sus pantallas", async ({ page, arrange }) => {
  const student = await arrange.student();

  await signIn(page, student.email, student.password);
  await page.goto("/teacher/students");
  await expect(page).toHaveURL(/\/calendar$/);
  await page.goto("/admin/users");
  await expect(page).toHaveURL(/\/calendar$/);

  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, TEACHER.email, TEACHER.password);
  await page.goto("/admin");
  await expect(page).toHaveURL(/\/teacher$/);
});

test("quien olvida la contraseña elige otra con el enlace del correo y entra con ella", async ({ page, arrange }) => {
  const student = await arrange.student();
  const newPassword = "a-brand-new-password";

  await page.goto("/login");
  await page.getByRole("link", { name: "He olvidado la contraseña" }).click();
  // The login form has an Email field too: filling before the new screen lands types into it.
  await expect(page.getByRole("heading", { name: "Recuperar la contraseña" })).toBeVisible();
  await page.getByLabel("Email").fill(student.email);
  await page.getByRole("button", { name: "Enviar enlace" }).click();
  await expect(page.getByRole("status")).toContainText("Si hay una cuenta con ese email");

  await page.goto(await linkSentTo(student.email, "/reset-password"));
  await page.getByLabel("Contraseña nueva").fill(newPassword);
  await page.getByRole("button", { name: "Cambiar contraseña" }).click();
  await expect(page.getByRole("status")).toContainText("Contraseña cambiada");

  await page.goto("/login");
  await page.getByLabel("Email").fill(student.email);
  await page.getByLabel("Contraseña").fill(student.password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "El email o la contraseña no son correctos." })).toBeVisible();

  await signIn(page, student.email, newPassword);
  await expect(page.getByRole("heading", { name: "Clases" })).toBeVisible();
});
