import { dayInCalendar, expect, openFromMenu, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

test("el profesor abre horas, añade a un alumno y crea una clase; al cancelarla, el alumno lo ve", async ({ page, browser, arrange }) => {
  const student = await arrange.student();
  const day = await arrange.freeDay();

  await signIn(page, TEACHER.email, TEACHER.password);
  await openFromMenu(page, "Horario");
  const extra = page.locator("form").filter({ has: page.getByRole("button", { name: "Añadir" }) });
  await extra.getByLabel("Día").fill(day);
  await extra.getByLabel("Tipo").selectOption({ label: "Horas extra" });
  await extra.getByLabel("Desde").fill("09:00");
  await extra.getByLabel("Hasta").fill("13:00");
  await extra.getByRole("button", { name: "Añadir" }).click();
  await expect(page.getByText("Disponible además de 09:00 a 13:00")).toBeVisible();

  await openFromMenu(page, "Alumnos");
  await page.getByLabel("Email del alumno").fill(student.email);
  await page.getByRole("button", { name: "Buscar" }).click();
  await page.getByRole("button", { name: "Añadir a mis alumnos" }).click();
  await expect(page.getByText("Alumno añadido")).toBeVisible();
  await expect(page.getByRole("listitem").filter({ hasText: student.email })).toContainText(student.fullName);

  await openFromMenu(page, "Calendario");
  await page.getByRole("button", { name: "Nueva clase" }).click();
  await page.getByLabel("Día").fill(day);
  await page.getByLabel("Hora").fill("10:00");
  await page.getByLabel("Grupo").check();
  await page.getByLabel("Plazas").fill("3");
  await page.getByRole("button", { name: "Crear clase" }).click();
  await expect(page.getByText("Clase creada")).toBeVisible();

  // The student, on their own phone, sees it and books it.
  const phone = await browser.newPage({ viewport: { width: 412, height: 915 } });
  await signIn(phone, student.email, student.password);
  const booked = (await dayInCalendar(phone, day)).getByRole("listitem").filter({ hasText: "10:00" });
  await booked.getByRole("button", { name: "Reservar" }).click();
  await expect(booked.getByText("Tienes plaza")).toBeVisible();

  await page.getByRole("button", { name: "Cerrar" }).click();
  const lesson = (await dayInCalendar(page, day)).getByRole("listitem").filter({ hasText: "10:00" });
  await expect(lesson).toContainText("2 plazas libres");
  await lesson.getByRole("link", { name: "Ver clase" }).click();
  await expect(page.getByText(student.fullName)).toBeVisible();
  await page.getByRole("button", { name: "Cancelar clase" }).click();
  await page.getByRole("button", { name: "Sí, cancelar la clase" }).click();
  await expect(page.getByText(/Cancelada, con/)).toBeVisible();

  await openFromMenu(phone, "Mis reservas");
  await expect(phone.getByRole("listitem").filter({ hasText: "10:00" })).toContainText("Cancelada por el profesor");
  await phone.close();
});

test("el profesor marca la asistencia en cuanto la clase empieza", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  // Attendance opens when the lesson starts, and the stack runs on the real clock: the lesson
  // starts a few seconds from now.
  const startsAt = new Date(Date.now() + 15_000);
  const lessonId = await arrange.lessonAt(startsAt);
  await arrange.book(student, lessonId);

  await signIn(page, TEACHER.email, TEACHER.password);
  await page.goto(`/teacher/lessons/${lessonId}`);
  await expect(page.getByText(student.fullName)).toBeVisible();
  await expect(page.getByText("La asistencia se marca cuando empiece la clase.")).toBeVisible();

  await page.waitForTimeout(Math.max(startsAt.getTime() - Date.now(), 0) + 1_000);
  await page.reload();
  await page.getByRole("group", { name: `Asistencia de ${student.fullName}` }).getByLabel("Vino", { exact: true }).check();
  await page.getByRole("button", { name: "Guardar asistencia" }).click();
  await expect(page.getByText("Asistencia guardada.")).toBeVisible();

  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, student.email, student.password);
  await openFromMenu(page, "Mis reservas");
  await expect(page.getByText("Asististe")).toBeVisible();
});
