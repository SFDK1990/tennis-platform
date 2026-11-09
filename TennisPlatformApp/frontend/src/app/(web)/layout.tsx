import type { ReactNode } from "react";

/**
 * The public website's frame. Its theme is the root layout's, which renders <html data-theme>
 * from the visitor's cookie (47-fase32, rule 4), the same choice as in the app.
 */
export default function WebLayout({ children }: { children: ReactNode }) {
  return (
    <div data-site="" className="min-h-screen bg-(--site-bg) text-(--site-text)">
      {children}
    </div>
  );
}
