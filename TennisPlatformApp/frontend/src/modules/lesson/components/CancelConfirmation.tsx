"use client";

import type { FormEvent, ReactNode } from "react";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";

interface CancelConfirmationProps {
  /** What will happen, said before it happens. */
  children: ReactNode;
  confirmLabel: string;
  pending: boolean;
  /** Only the teacher writes to the students; the administrator's route takes no reason. */
  withReason: boolean;
  disabled?: boolean;
  onConfirm: (reason: string | undefined) => void;
  onBack?: () => void;
}

/**
 * The last step before cancelling one lesson or a whole day: what it affects, the reason the
 * students will read, and the button that does it.
 */
export function CancelConfirmation({
  children,
  confirmLabel,
  pending,
  withReason,
  disabled = false,
  onConfirm,
  onBack,
}: CancelConfirmationProps) {
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const reason = String(new FormData(event.currentTarget).get("reason") ?? "").trim();
    onConfirm(reason || undefined);
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-3">
      {children}
      {withReason ? (
        <Field
          label="Motivo para los alumnos"
          name="reason"
          maxLength={200}
          placeholder="Por ejemplo, lluvia"
          hint="Opcional. Va en el correo y no se guarda."
        />
      ) : null}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" variant="danger" pending={pending} disabled={disabled}>
          {confirmLabel}
        </Button>
        {onBack ? (
          <Button type="button" variant="quiet" onClick={onBack}>
            No
          </Button>
        ) : null}
      </div>
    </form>
  );
}
