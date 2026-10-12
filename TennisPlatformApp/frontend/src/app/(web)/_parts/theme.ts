export const THEME_COOKIE = "theme";

export type Theme = "light" | "dark";

/** The visitor's own choice, if they made one; anything else means "follow the system". */
export function chosenTheme(value: string | undefined): Theme | undefined {
  return value === "light" || value === "dark" ? value : undefined;
}
