import { expectAccessible } from "./support/axe";
import { signIn, test } from "./support/fixtures";
import { PUBLIC_SCREENS } from "./support/screens";
import { ADMIN, TEACHER } from "./support/stack";

/**
 * axe on every screen of the teacher and the administrator, at a desk (45-fase30-analisis-pwa-
 * accesibilidad.md, rule 8). The student's screens are in accessibility.student.spec.ts, on a
 * phone. Each screen is opened with something in it, so its lists and forms are looked at too.
 */

test("axe no encuentra fallos graves en las pantallas sin sesión", async ({ page }) => {
  for (const path of PUBLIC_SCREENS) {
    await expectAccessible(page, path);
  }
});

test("axe no encuentra fallos graves en las pantallas del profesor", async ({ page, arrange }) => {
  const student = await arrange.student({ managed: true });
  const day = await arrange.freeDay();
  const lessonId = await arrange.lesson({ date: day, time: "10:00", capacity: 2 });
  await arrange.book(student, lessonId);

  await signIn(page, TEACHER.email, TEACHER.password);
  for (const path of [
    "/teacher",
    "/teacher/calendar",
    "/teacher/availability",
    "/teacher/students",
    `/teacher/students/${student.userId}`,
    `/teacher/lessons/${lessonId}`,
    "/teacher/passes",
    "/teacher/fees",
    "/teacher/messages",
    "/teacher/summary",
    "/profile",
  ]) {
    await expectAccessible(page, path);
  }
});

test("axe no encuentra fallos graves en las pantallas del admin", async ({ page, arrange }) => {
  const day = await arrange.freeDay();
  const lessonId = await arrange.lesson({ date: day, time: "10:00" });

  await signIn(page, ADMIN.email, ADMIN.password);
  for (const path of ["/admin", "/admin/users", "/admin/lessons", `/admin/lessons/${lessonId}`, "/profile"]) {
    await expectAccessible(page, path);
  }
});
