import type { ReactNode } from "react";

type Tone = "neutral" | "court" | "success" | "warning" | "fault";

const TONES: Record<Tone, string> = {
  neutral: "bg-line-soft text-muted",
  court: "bg-court-tint text-court",
  success: "bg-surround-tint text-surround-ink",
  warning: "bg-warning-tint text-warning",
  fault: "bg-fault-tint text-fault",
};

/** A short state next to what it describes: "Completa", "Tienes plaza". */
export function Chip({ tone = "neutral", children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span className={`inline-flex h-6 items-center whitespace-nowrap rounded-full px-2.5 text-[13px] font-semibold ${TONES[tone]}`}>
      {children}
    </span>
  );
}
