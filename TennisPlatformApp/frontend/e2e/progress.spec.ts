import { expect, openFromMenu, signIn, test } from "./support/fixtures";
import { TEACHER } from "./support/stack";

/** Fase 29: the teacher writes the objectives of a level and marks one; the student sees it. */
test("el profesor pone objetivos a un nivel, marca uno en la ficha y el alumno ve su progreso y el aviso",
  async ({ page, arrange }) => {
    const student = await arrange.student({ managed: true });
    const level = await arrange.newLevel();
    await arrange.studentLevel(student, level.id);
    arrange.clearProgressAfterwards(student);

    await signIn(page, TEACHER.email, TEACHER.password);
    await openFromMenu(page, "Perfil");
    const row = page.getByRole("listitem").filter({ has: page.getByLabel(`Nuevo objetivo de ${level.name}`) });
    for (const title of ["Peloteo de diez golpes", "Saque por arriba"]) {
      await row.getByLabel(`Nuevo objetivo de ${level.name}`).fill(title);
      await row.getByRole("button", { name: "Añadir objetivo" }).click();
      await expect(row.getByRole("list", { name: `Objetivos de ${level.name}` }).getByRole("textbox"))
        .toHaveCount(title === "Saque por arriba" ? 2 : 1);
    }

    await openFromMenu(page, "Alumnos");
    await page.getByRole("link", { name: student.fullName }).click();
    const rally = page.getByRole("checkbox", { name: "Peloteo de diez golpes" });
    await rally.check();
    await expect(rally).toBeChecked();
    await expect(page.getByRole("group", { name: new RegExp(level.name) })).toContainText("1 de 2");

    await page.getByRole("button", { name: "Salir" }).click();
    await signIn(page, student.email, student.password);
    await openFromMenu(page, "Perfil");
    const progress = page.getByRole("region", { name: "Mi progreso" });
    await expect(progress.getByRole("heading", { name: `Tu nivel: ${level.name}` })).toBeVisible();
    await expect(progress.getByRole("progressbar", { name: "1 de 2 objetivos conseguidos" })).toBeVisible();
    await expect(progress.getByText("Peloteo de diez golpes, conseguido")).toBeAttached();

    await openFromMenu(page, "Avisos");
    await expect(page.getByRole("heading", { name: "Objetivo conseguido: Peloteo de diez golpes" })).toBeVisible();
  });
