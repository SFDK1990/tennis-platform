/**
 * The visitor's theme, one choice for the website and the app (31-fase31, decision 8; 47-fase32,
 * decision 1). It lives in a cookie so the server can render <html data-theme> with it: the page
 * arrives in its colours, with no script before hydration and no flash of the other theme.
 * Without the cookie, the system's preference decides.
 */
export const THEME_COOKIE = "theme";

export type Theme = "light" | "dark";

const ONE_YEAR = 60 * 60 * 24 * 365;

/** The visitor's own choice, if they made one; anything else means "follow the system". */
export function chosenTheme(value: string | undefined): Theme | undefined {
  return value === "light" || value === "dark" ? value : undefined;
}

/** The theme in force on this page: the choice on <html>, or else the system's. */
export function themeInForce(): Theme {
  return chosenTheme(document.documentElement.dataset.theme)
    ?? (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
}

/** Applies a choice to this page and remembers it; `undefined` goes back to the system's. */
export function chooseTheme(theme: Theme | undefined): void {
  const root = document.documentElement;
  const secure = location.protocol === "https:" ? "; Secure" : "";
  if (theme) {
    root.dataset.theme = theme;
    document.cookie = `${THEME_COOKIE}=${theme}; Path=/; Max-Age=${ONE_YEAR}; SameSite=Lax${secure}`;
  } else {
    delete root.dataset.theme;
    document.cookie = `${THEME_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax${secure}`;
  }
}
