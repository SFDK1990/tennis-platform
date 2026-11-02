import { expect, openFromMenu, signIn, test } from "./support/fixtures";
import { todayIn } from "@/shared/time";
import { TEACHER, ZONE } from "./support/stack";

/**
 * Fase 28: the teacher makes a fixed group of a weekly series, the student joins it from "Mis
 * reservas" and gets a seat in every lesson, and the teacher records the month as paid. A month
 * is listed once it has started, or picked by hand ahead of time: the board goes to the lesson's.
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
  expect(await arrange.confirmedBookingsOf(lessons[0])).toBe(1);
  expect(await arrange.confirmedBookingsOf(lessons[1])).toBe(1);

  // The month of the first lesson: this one, or the next when the free day falls in it.
  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, TEACHER.email, TEACHER.password);
  await openFromMenu(page, "Mensualidades");
  if (day.slice(0, 7) !== todayIn(ZONE).slice(0, 7)) {
    await page.getByRole("button", { name: "Mes siguiente" }).click();
  }
  const month = page.getByTestId("fee-line").filter({ hasText: student.fullName }).filter({ hasText: name });
  await expect(month).toContainText("Pendiente");
  await month.getByRole("button", { name: "Apuntar pago" }).click();
  await month.getByLabel("Cómo").selectOption("BIZUM");
  await month.getByRole("button", { name: "Pagado hoy" }).click();
  await expect(month).toContainText("Pagada");
  await expect(month).toContainText("bizum, 45 €");
});
