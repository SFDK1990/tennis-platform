import { linkSentTo } from "./support/mailbox";
import { dayInCalendar, expect, openFromMenu, signIn, test } from "./support/fixtures";
import { STUDENT_PASSWORD, uniqueEmail } from "./support/stack";

test("un alumno se registra, verifica su email con el enlace del correo y completa su perfil", async ({ page }) => {
  const email = uniqueEmail();

  await page.goto("/register");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Contraseña").fill(STUDENT_PASSWORD);
  await page.getByRole("button", { name: "Crear cuenta" }).click();
  await expect(page.getByRole("status")).toContainText("Te hemos enviado un correo");

  await page.goto(await linkSentTo(email, "/verify-email"));
  await page.getByRole("button", { name: "Verificar mi email" }).click();
  await expect(page.getByRole("status")).toContainText("Email verificado");

  await signIn(page, email, STUDENT_PASSWORD);
  await expect(page.getByRole("heading", { name: "Clases" })).toBeVisible();
  await page.getByRole("link", { name: "tu perfil" }).click();
  await page.getByLabel("Nombre y apellidos").fill("Lucía Ferrer");
  await page.getByRole("button", { name: "Guardar datos" }).click();
  await expect(page.getByText("Datos guardados.")).toBeVisible();
  await expect(page.getByRole("link", { name: "tu perfil" })).toBeHidden();
});

test("un alumno reserva una clase de su profesor y la cancela", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  const day = await arrange.freeDay();
  await arrange.lesson({ date: day, time: "10:00", capacity: 2 });

  await signIn(page, student.email, student.password);
  const lesson = (await dayInCalendar(page, day)).getByRole("listitem").filter({ hasText: "10:00" });
  await expect(lesson).toContainText("2 plazas libres");
  await lesson.getByRole("button", { name: "Reservar" }).click();
  await expect(lesson.getByText("Tienes plaza")).toBeVisible();
  await expect(lesson).toContainText("1 plaza libre");

  await openFromMenu(page, "Mis reservas");
  await expect(page.getByRole("listitem").filter({ hasText: "10:00" })).toContainText("Confirmada");

  await openFromMenu(page, "Clases");
  const again = (await dayInCalendar(page, day)).getByRole("listitem").filter({ hasText: "10:00" });
  await again.getByRole("button", { name: "Cancelar reserva" }).click();
  await again.getByRole("button", { name: "Sí, cancelar" }).click();
  await expect(again.getByRole("button", { name: "Reservar" })).toBeVisible();
  await expect(again).toContainText("2 plazas libres");
});

test("con la clase llena, otro alumno ya no puede reservar", async ({ page, arrange }) => {
  const first = await arrange.student({ managed: true });
  const second = await arrange.student({ managed: true });
  const day = await arrange.freeDay();
  const lessonId = await arrange.lesson({ date: day, time: "11:00", capacity: 1 });
  await arrange.book(first, lessonId);

  await signIn(page, second.email, second.password);
  const lesson = (await dayInCalendar(page, day)).getByRole("listitem").filter({ hasText: "11:00" });
  await expect(lesson).toContainText("Completa");
  await expect(lesson).toContainText("Sin plazas");
  await expect(lesson.getByRole("button", { name: "Reservar" })).toHaveCount(0);
});
