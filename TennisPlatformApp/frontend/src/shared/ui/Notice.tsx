import type { ReactNode } from "react";
import { ApiError } from "@/shared/api/errors";
import { messageFor } from "@/shared/api/messages";

type Tone = "info" | "success" | "error";

const TONES: Record<Tone, string> = {
  info: "border-court/30 bg-court-tint/60",
  success: "border-surround/40 bg-surround-tint/60 text-surround-ink",
  error: "border-fault/40 bg-fault-tint/60 text-fault",
};

export function Notice({ tone = "info", children }: { tone?: Tone; children: ReactNode }) {
  return (
    <div role={tone === "error" ? "alert" : "status"} className={`rounded-lg border px-4 py-3 ${TONES[tone]}`}>
      {children}
    </div>
  );
}

/** Renders nothing without an error, so it can sit under any form or action. */
export function ErrorNotice({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }
  const text = error instanceof ApiError ? messageFor(error) : "No se ha podido conectar con el servidor.";
  return <Notice tone="error">{text}</Notice>;
}
