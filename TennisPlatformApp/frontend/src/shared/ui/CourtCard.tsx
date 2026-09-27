import type { ReactNode } from "react";

/**
 * The one bold gesture of the design: the lesson that matters now, drawn on a court seen from
 * above. The lines are decoration only; the content sits over them.
 */
export function CourtCard({ label, tone = "court", children }: { label: string; tone?: "court" | "navy"; children: ReactNode }) {
  return (
    <section aria-label={label}
      className={`relative overflow-hidden rounded-xl px-5 py-5 text-white ${tone === "court" ? "bg-court" : "bg-navy"}`}>
      <div aria-hidden className="pointer-events-none absolute inset-2.5 rounded-[3px] border-2 border-white/25" />
      <div aria-hidden className="pointer-events-none absolute inset-y-2.5 left-[64%] w-0.5 bg-white/25" />
      <div aria-hidden className="pointer-events-none absolute left-2.5 right-[36%] top-1/2 h-0.5 bg-white/25" />
      <div className="relative flex flex-col gap-3">{children}</div>
    </section>
  );
}
