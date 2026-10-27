import { dayInCalendar, expect, openFromMenu, signIn, test } from "./support/fixtures";
import { addDays } from "@/shared/time";
import { TEACHER, ZONE } from "./support/stack";

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

  await openFromMenu(page, "Agenda");
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
  await openFromMenu(phone, "Clases");
  const booked = (await dayInCalendar(phone, day)).getByRole("listitem").filter({ hasText: "10:00" });
  await booked.getByRole("button", { name: "Reservar" }).click();
  await expect(booked.getByText("Tienes plaza")).toBeVisible();

  // The booking came from another browser, and nothing makes this page read the week again: if
  // the test's day is in the current week (a fresh CI database hands out days from today + 2),
  // the agenda keeps the count it had. Only moving to another week fetched it anew. Reloading is
  // what the teacher would do.
  await page.getByRole("button", { name: "Cerrar" }).click();
  await page.reload();
  const lesson = (await dayInCalendar(page, day)).getByRole("listitem").filter({ hasText: "10:00" });
  await expect(lesson).toContainText("2 plazas libres");
  // Its row comes from a class generated ahead in globals.css; without it the block drops to the top.
  await expect(lesson).not.toHaveCSS("grid-row-start", "auto");
  await lesson.getByRole("link").click();
  await expect(page.getByText(student.fullName)).toBeVisible();
  await page.getByRole("button", { name: "Cancelar clase" }).click();
  await page.getByRole("button", { name: "Sí, cancelar la clase" }).click();
  await expect(page.getByText(/Cancelada, con/)).toBeVisible();

  await openFromMenu(phone, "Mis reservas");
  await expect(phone.getByRole("listitem").filter({ hasText: "10:00" })).toContainText("Cancelada por el profesor");
  await phone.close();
});

test("el profesor pasa lista desde Hoy en cuanto la clase empieza", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  // Attendance opens when the lesson starts, and the stack runs on the real clock: the lesson
  // starts a few seconds from now.
  const startsAt = new Date(Date.now() + 15_000);
  const lessonId = await arrange.lessonAt(startsAt);
  await arrange.book(student, lessonId);

  // Hoy is where the teacher lands, and this lesson is the one about to start.
  await signIn(page, TEACHER.email, TEACHER.password);
  await expect(page.getByRole("heading", { name: "Hoy", exact: true })).toBeVisible();
  await expect(page.getByRole("region", { name: "Próxima clase" })).toBeVisible();
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

test("el profesor sube las plazas de una clase de grupo y le añade notas", async ({ page, arrange }) => {
  const lessonId = await arrange.lesson({ date: await arrange.freeDay(), time: "12:00", capacity: 3 });

  await signIn(page, TEACHER.email, TEACHER.password);
  await page.goto(`/teacher/lessons/${lessonId}`);
  await page.getByRole("button", { name: "Editar" }).click();
  const form = page.getByRole("form", { name: "Editar la clase" });
  await form.getByLabel("Plazas").fill("5");
  await form.getByLabel("Notas").fill("Traed agua");
  await form.getByRole("button", { name: "Guardar cambios" }).click();

  await expect(page.getByText("Clase de grupo, 5 plazas")).toBeVisible();
  await expect(page.getByText("Traed agua")).toBeVisible();
});

/** Fase 22: a weekly lesson for four weeks, skipping the week that clashes with another lesson. */
test("el profesor crea una clase semanal de 4 semanas y salta la fecha que choca", async ({ page, arrange }) => {
  const day = await arrange.freeDay();
  const weeks = [0, 1, 2, 3].map((week) => addDays(day, 7 * week));
  weeks.slice(1).forEach((date) => arrange.clearAfterwards(date));
  await arrange.lesson({ date: weeks[2], time: "06:00" });

  await signIn(page, TEACHER.email, TEACHER.password);
  await openFromMenu(page, "Agenda");
  await page.getByRole("button", { name: "Nueva clase" }).click();
  await page.getByLabel("Día").fill(day);
  await page.getByLabel("Hora").fill("06:00");
  await page.getByLabel("Repetir cada semana").check();
  await page.getByLabel("Durante").selectOption("4");
  await page.getByRole("button", { name: "Ver las fechas" }).click();

  const dates = page.getByRole("region", { name: "Nueva clase" }).getByRole("listitem");
  await expect(dates).toHaveCount(4);
  await expect(dates.nth(2)).toContainText("Choca con otra clase");
  await expect(dates.nth(2).getByRole("checkbox")).toBeDisabled();
  // 06:00 is outside the hours: those start unticked, and can be ticked anyway.
  for (const week of [0, 1, 3]) {
    await dates.nth(week).getByRole("checkbox").check();
  }
  await page.getByRole("button", { name: "Crear 3 clases" }).click();
  await expect(page.getByText("Serie creada: 3 clases")).toBeVisible();
});

/** Fase 25: the month in numbers, and the month before it. */
test("el profesor abre el resumen del mes y va al mes anterior", async ({ page }) => {
  await signIn(page, TEACHER.email, TEACHER.password);
  await openFromMenu(page, "Resumen");
  const month = new Intl.DateTimeFormat("es-ES", { timeZone: ZONE, month: "long", year: "numeric" }).format(new Date());
  await expect(page.getByText(month, { exact: false })).toBeVisible();
  await expect(page.getByText("hasta hoy")).toBeVisible();
  await expect(page.getByText("Clases dadas")).toBeVisible();
  await expect(page.getByRole("button", { name: "Mes siguiente" })).toBeDisabled();

  await page.getByRole("button", { name: "Mes anterior" }).click();
  await expect(page.getByText("hasta hoy")).toHaveCount(0);
  await page.getByRole("button", { name: "Mes siguiente" }).click();
  await expect(page.getByText("hasta hoy")).toBeVisible();
});
