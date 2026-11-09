import { expectAccessible } from "./support/axe";
import { signIn, test } from "./support/fixtures";
import { PUBLIC_SCREENS } from "./support/screens";

/**
 * axe on every screen of the student, on a phone: a Pixel in Chromium and an iPhone in WebKit
 * (45-fase30-analisis-pwa-accesibilidad.md, rules 8 and 10), in light and in dark. The student has a level, a booking
 * and a message, so the screens are not empty.
 */

// Both themes (47-fase32-analisis-modo-oscuro.md): the same screens, once with the system in dark.
for (const colorScheme of ["light", "dark"] as const) {
  test.describe(colorScheme === "light" ? "en claro" : "en oscuro", () => {
    test.use({ colorScheme });

    test("axe no encuentra fallos graves en las pantallas sin sesión, en el móvil", async ({ page }) => {
      for (const path of PUBLIC_SCREENS) {
        await expectAccessible(page, path);
      }
    });

    test("axe no encuentra fallos graves en las pantallas del alumno", async ({ page, arrange }) => {
      const student = await arrange.student({ managed: true });
      const day = await arrange.freeDay();
      const lessonId = await arrange.lesson({ date: day, time: "10:00", capacity: 2 });
      await arrange.book(student, lessonId);

      await signIn(page, student.email, student.password);
      for (const path of ["/home", "/calendar", "/bookings", "/passes", "/messages", "/profile"]) {
        await expectAccessible(page, path);
      }
    });
  });
}
