import { expect, openFromMenu, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

/**
 * Fase 28: the teacher makes a fixed group of a weekly series, the student joins it from "Mis
 * reservas" and gets a seat in every lesson with the month to pay, and the teacher records the
 * payment from the student's record.
 */
test("el profesor crea un grupo fijo, el alumno se apunta y el profesor apunta la mensualidad", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  const day = await arrange.freeDay();
  const lessons = await arrange.weeklySeries({ date: day, time: "07:30" });
  const name = `E2E grupo ${Math.random().toString(36).slice(2, 8)}`;
  arrange.deleteGroupAfterwards(name);

  await signIn(page, TEACHER.email, TEACHER.password);
  await openFromMenu(page, "Mensualidades");
  const groups = page.getByRole("region", { name: "Grupos fijos" });
  await groups.getByLabel("Nombre del grupo").fill(name);
  await groups.getByLabel("Cuota al mes (€)").fill("45");
  await groups.getByRole("button", { name: "Crear grupo" }).click();
  await expect(groups.getByRole("article", { name })).toBeVisible();

  await page.goto(`/teacher/lessons/${lessons[0]}`);
  const attach = page.getByRole("region", { name: "Grupo fijo" });
  await attach.getByLabel("Añadir la serie al grupo").selectOption({ label: name });
  await attach.getByRole("button", { name: "Añadir" }).click();
  await expect(attach).toContainText(`Esta serie es del grupo ${name}`);

  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, student.email, student.password);
  await openFromMenu(page, "Mis reservas");
  const group = page.getByRole("article", { name });
  await group.getByRole("button", { name: "Apuntarme" }).click();
  await group.getByRole("button", { name: "Apuntarme" }).click();
  await expect(group.getByRole("button", { name: "Darme de baja" })).toBeVisible();
  await expect(group).toContainText("Pendiente");
  expect(await arrange.confirmedBookingsOf(lessons[0])).toBe(1);
  expect(await arrange.confirmedBookingsOf(lessons[1])).toBe(1);

  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, TEACHER.email, TEACHER.password);
  await openFromMenu(page, "Alumnos");
  await page.getByRole("link", { name: student.fullName }).click();
  const month = page.getByRole("list", { name: "Mensualidades" }).getByTestId("fee-line").first();
  await expect(month).toContainText("Pendiente");
  await month.getByRole("button", { name: "Apuntar pago" }).click();
  await month.getByLabel("Cómo").selectOption("BIZUM");
  await month.getByRole("button", { name: "Pagado hoy" }).click();
  await expect(month).toContainText("Pagada");
  await expect(month).toContainText("bizum, 45 €");
});
