import type { ReactNode } from "react";

/** The court is the brand: two chalk lines framing the door in. */
export default function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-court px-4 py-12">
      <div className="w-full max-w-md border-y-2 border-white/80 py-8">
        <p className="mb-6 text-center font-display text-4xl font-semibold text-white">Tennis Platform</p>
        <div className="rounded-md bg-paper p-6">{children}</div>
      </div>
    </main>
  );
}
