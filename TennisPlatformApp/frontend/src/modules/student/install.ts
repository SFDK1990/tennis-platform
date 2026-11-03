/** Which steps install the app on this device, or none when it is installed or cannot be. */
export type InstallSteps = "ios" | "android" | null;

/**
 * 45-fase30-analisis-pwa-accesibilidad.md, decision 2. Chrome on Android offers to install the app
 * by itself, but the student may have dismissed it; Safari never offers it. On a computer the app
 * is not what a student installs, so nothing is shown. iPadOS says it is a Mac, and gives itself
 * away by its touch screen.
 */
export function installStepsFor(userAgent: string, installed: boolean, touchPoints: number): InstallSteps {
  if (installed) return null;
  if (/iPhone|iPad|iPod/.test(userAgent) || (/Macintosh/.test(userAgent) && touchPoints > 1)) return "ios";
  if (/Android/.test(userAgent)) return "android";
  return null;
}
