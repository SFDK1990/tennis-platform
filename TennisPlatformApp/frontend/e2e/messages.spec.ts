import { expectEmail } from "./support/mailbox";
import { expect, openFromMenu, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

/** Fase 24: the teacher writes to a level, and its student reads it on the board and by email. */
test("el profesor escribe a un nivel y el alumno lo lee en sus avisos y en su correo", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  const level = await arrange.newLevel();
  await arrange.studentLevel(student, level.id);
  const subject = `Cambio de pista ${Math.random().toString(36).slice(2, 8)}`;
  arrange.deleteMessageAfterwards(subject);

  await signIn(page, TEACHER.email, TEACHER.password);
  await openFromMenu(page, "Mensajes");
  await page.getByRole("radio", { name: `Nivel ${level.name}` }).check();
  await page.getByLabel("Asunto").fill(subject);
  await page.getByRole("textbox", { name: "Mensaje" }).fill("Esta semana jugamos en la pista 2.");
  await page.getByRole("button", { name: "Enviar" }).click();
  await expect(page.getByText("Enviado a 1 alumno.")).toBeVisible();
  await expect(page.getByRole("heading", { name: subject })).toBeVisible();
  await expectEmail(student.email, subject);

  await page.getByRole("button", { name: "Salir" }).click();
  await signIn(page, student.email, student.password);
  await expect(page.getByRole("heading", { name: "Último aviso" })).toBeVisible();
  await openFromMenu(page, "Avisos");
  const message = page.getByRole("article").filter({ has: page.getByRole("heading", { name: subject }) });
  await expect(message).toContainText("Esta semana jugamos en la pista 2.");
});
