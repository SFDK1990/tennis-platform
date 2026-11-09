"use client";

import { useSyncExternalStore } from "react";
import { chooseTheme, chosenTheme, type Theme } from "@/shared/theme";

type Choice = Theme | "system";

const CHOICES: { value: Choice; label: string }[] = [
  { value: "system", label: "Como el móvil" },
  { value: "light", label: "Claro" },
  { value: "dark", label: "Oscuro" },
];

/** The choice lives on <html>, where the website's button can change it too. */
function subscribe(onChange: () => void) {
  const observer = new MutationObserver(onChange);
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
  return () => observer.disconnect();
}

function current(): Choice {
  return chosenTheme(document.documentElement.dataset.theme) ?? "system";
}

/**
 * Light, dark or whatever the device says (47-fase32-analisis-modo-oscuro.md, decision 1). The
 * same cookie as the website's button, so one choice holds for both. The server does not know
 * it while rendering this client page, so nothing is checked until the browser says.
 */
export function AppearancePicker() {
  const selected = useSyncExternalStore(subscribe, current, () => null);
  return (
    <section aria-labelledby="appearance-heading" className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
      <h2 id="appearance-heading" className="font-display text-2xl font-semibold leading-none">Apariencia</h2>
      <fieldset className="flex flex-col gap-2">
        <legend className="sr-only">Tema</legend>
        <div className="flex flex-wrap gap-2">
          {CHOICES.map((choice) => (
            <label key={choice.value}
              className="flex min-h-11 cursor-pointer items-center gap-2 rounded-lg border-[1.5px] border-line px-3.5 has-checked:border-link has-checked:bg-court-tint has-checked:font-semibold">
              <input type="radio" name="theme" value={choice.value} checked={choice.value === selected}
                onChange={() => chooseTheme(choice.value === "system" ? undefined : choice.value)}
                className="accent-link" />
              {choice.label}
            </label>
          ))}
        </div>
      </fieldset>
      <p className="text-sm text-muted">Vale también para la web de la escuela.</p>
    </section>
  );
}
