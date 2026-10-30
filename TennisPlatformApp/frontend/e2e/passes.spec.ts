import { dayInCalendar, expect, openFromMenu, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

/**
 * Fase 27: the teacher records a pass the student paid, the student books with it, sees their
 * balance go down, and gets the lesson back when they cancel in time.
 */
test("el profesor apunta un bono, el alumno reserva con él y recupera la clase al cancelar", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  const type = await arrange.passType(3);
  await arrange.requirePasses();
  const day = await arrange.freeDay();
  await arrange.lesson({ date: day, time: "10:00", capacity: 4 });

  await signIn(page, TEACHER.email, TEACHER.password);
  await openFromMenu(page, "Alumnos");
  await page.getByRole("link", { name: student.fullName }).click();
  const record = page.getByRole("form", { name: "Apuntar un bono" });
  await record.getByLabel("Bono").selectOption({ label: `${type.name}: 3 clases de grupo, 30 €, 3 meses` });
  await record.getByLabel("Cómo").selectOption("BIZUM");
  await record.getByRole("button", { name: "Apuntar bono" }).click();
  const pass = page.getByRole("listitem").filter({ hasText: type.name });
  await expect(pass).toContainText("Quedan 3 de 3 clases de grupo");
  await expect(pass).toContainText("Activo");

  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, student.email, student.password);
  await openFromMenu(page, "Clases");
  const lesson = (await dayInCalendar(page, day)).getByRole("listitem").filter({ hasText: "10:00" });
  await lesson.getByRole("button", { name: "Reservar" }).click();
  await expect(lesson.getByText("Tienes plaza")).toBeVisible();

  await openFromMenu(page, "Mis reservas");
  const balance = page.getByRole("region", { name: "Tus bonos" });
  await expect(balance).toContainText("Te quedan 2 clases de grupo");

  await openFromMenu(page, "Clases");
  const again = (await dayInCalendar(page, day)).getByRole("listitem").filter({ hasText: "10:00" });
  await again.getByRole("button", { name: "Cancelar reserva" }).click();
  await again.getByRole("button", { name: "Sí, cancelar" }).click();
  await expect(again.getByRole("button", { name: "Reservar" })).toBeVisible();

  await openFromMenu(page, "Mis reservas");
  await expect(page.getByRole("region", { name: "Tus bonos" })).toContainText("Te quedan 3 clases de grupo");
});
