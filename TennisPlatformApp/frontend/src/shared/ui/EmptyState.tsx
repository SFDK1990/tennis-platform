import type { ReactNode } from "react";

/** An empty screen is an invitation to act: an empty court, what is missing, and the way in. */
export function EmptyState({ title, children, actions }: { title: string; children?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-line bg-paper px-6 py-8 text-center">
      <svg viewBox="0 0 220 104" aria-hidden className="h-20 w-auto fill-none stroke-court stroke-2">
        <rect x="1" y="1" width="218" height="102" rx="3" />
        <path d="M1 14H219M1 90H219M56 14V90M164 14V90M56 52H164" />
        <path d="M110 1V103" strokeDasharray="4 4" />
      </svg>
      <h2 className="font-display text-2xl font-semibold">{title}</h2>
      {children ? <div className="max-w-sm text-muted">{children}</div> : null}
      {actions ? <div className="mt-1 flex flex-wrap justify-center gap-2">{actions}</div> : null}
    </div>
  );
}
