"use client";

import type { MouseEvent } from "react";
import { THEME_COOKIE } from "./theme";

const ONE_YEAR = 60 * 60 * 24 * 365;

/**
 * Switches the website between light and dark (31-fase31, decision 8). Which icon and label
 * show is left to CSS, which already knows the system's preference: deciding it here would
 * need that preference during the server render, where it does not exist.
 */
export function ThemeToggle() {
  function toggle(event: MouseEvent<HTMLButtonElement>) {
    const site = event.currentTarget.closest<HTMLElement>("[data-site]");
    if (!site) {
      return;
    }
    const dark = site.dataset.theme
      ? site.dataset.theme === "dark"
      : matchMedia("(prefers-color-scheme: dark)").matches;
    const next = dark ? "light" : "dark";
    spreadFrom(event.currentTarget, () => {
      site.dataset.theme = next;
    });
    const secure = location.protocol === "https:" ? "; Secure" : "";
    document.cookie = `${THEME_COOKIE}=${next}; Path=/; Max-Age=${ONE_YEAR}; SameSite=Lax${secure}`;
  }

  return (
    <button
      type="button"
      onClick={toggle}
      className="flex size-11 items-center justify-center rounded-full border border-white/40 text-white hover:border-white"
    >
      <span className="theme-to-dark flex items-center">
        <span className="sr-only">Cambiar a modo oscuro</span>
        <svg viewBox="0 0 24 24" aria-hidden className="size-5 fill-none stroke-current stroke-[1.8]">
          <path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z" strokeLinejoin="round" />
        </svg>
      </span>
      <span className="theme-to-light flex items-center">
        <span className="sr-only">Cambiar a modo claro</span>
        <svg viewBox="0 0 24 24" aria-hidden className="size-5 fill-none stroke-current stroke-[1.8]">
          <circle cx="12" cy="12" r="4" />
          <path
            d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"
            strokeLinecap="round"
          />
        </svg>
      </span>
    </button>
  );
}

/**
 * The new theme spreads out from the button (decision 22). A Web Animation on the View
 * Transition, not a style attribute, so the CSP has nothing to block; where either is missing,
 * or the visitor asked for less motion, the theme simply changes.
 */
function spreadFrom(button: HTMLElement, change: () => void) {
  if (!document.startViewTransition || matchMedia("(prefers-reduced-motion: reduce)").matches) {
    change();
    return;
  }
  const { left, top, width, height } = button.getBoundingClientRect();
  const x = left + width / 2;
  const y = top + height / 2;
  const radius = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y));
  document.startViewTransition(change).ready.then(() => {
    document.documentElement.animate(
      { clipPath: [`circle(0 at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
      { duration: 550, easing: "cubic-bezier(0.2, 0.7, 0.2, 1)", pseudoElement: "::view-transition-new(root)" },
    );
  });
}
