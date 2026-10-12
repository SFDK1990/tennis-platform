import { cookies } from "next/headers";
import type { ReactNode } from "react";
import { chosenTheme, THEME_COOKIE } from "./_parts/theme";

/**
 * The public website's frame. The theme is decided here, on the server, from the visitor's
 * cookie: the page arrives already in its colours, with no script that would have to run before
 * hydration (the nonce CSP would have to let it through) and no flash of the other theme.
 */
export default async function WebLayout({ children }: { children: ReactNode }) {
  const theme = chosenTheme((await cookies()).get(THEME_COOKIE)?.value);
  return (
    <div data-site="" data-theme={theme} className="min-h-screen bg-(--site-bg) text-(--site-text)">
      {children}
    </div>
  );
}
