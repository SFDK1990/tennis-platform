"use client";

import { useSyncExternalStore } from "react";
import { installStepsFor, type InstallSteps } from "@/modules/student/install";

const subscribe = () => () => {};

function stepsOnThisDevice(): InstallSteps {
  // Safari says it in navigator.standalone; every other browser, in the display mode.
  const installed =
    window.matchMedia("(display-mode: standalone)").matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true;
  return installStepsFor(navigator.userAgent, installed, navigator.maxTouchPoints);
}

/**
 * How to put the app on the phone (45-fase30-analisis-pwa-accesibilidad.md, decision 2): a few
 * lines in the profile, only on a phone where it is not installed yet. The server cannot know the
 * device, so it renders nothing and the browser decides.
 */
export function InstallApp() {
  const steps = useSyncExternalStore(subscribe, stepsOnThisDevice, () => null);
  if (!steps) return null;

  return (
    <section aria-labelledby="install-heading" className="flex flex-col gap-3 rounded-xl border border-line bg-paper p-5">
      <h2 id="install-heading" className="font-display text-2xl font-semibold leading-none">Instálala en tu móvil</h2>
      <p className="text-muted">Tendrás la app en tu pantalla de inicio, como cualquier otra, y entrarás sin abrir el navegador.</p>
      {steps === "ios" ? (
        <ol className="flex list-decimal flex-col gap-1 pl-6">
          <li>Ábrela en Safari.</li>
          <li>Toca el botón <span className="font-semibold">Compartir</span>, el cuadrado con la flecha hacia arriba.</li>
          <li>Elige <span className="font-semibold">Añadir a pantalla de inicio</span>.</li>
        </ol>
      ) : (
        <ol className="flex list-decimal flex-col gap-1 pl-6">
          <li>Ábrela en Chrome.</li>
          <li>Toca el menú <span className="font-semibold">⋮</span>, arriba a la derecha.</li>
          <li>Elige <span className="font-semibold">Instalar aplicación</span> o <span className="font-semibold">Añadir a pantalla de inicio</span>.</li>
        </ol>
      )}
    </section>
  );
}
