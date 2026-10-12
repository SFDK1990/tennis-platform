"use client";

import { useCancelDay } from "@/modules/lesson/api";
import { CancelConfirmation } from "@/modules/lesson/components/CancelConfirmation";
import type { LocalDate } from "@/shared/time";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

interface CancelDayFormProps {
  date: LocalDate;
  today: LocalDate;
  onDateChange: (date: LocalDate) => void;
  /** What the day holds, worked out by the page from the calendar; undefined while it loads. */
  affected: { lessons: number; bookings: number } | undefined;
}

const plural = (count: number, one: string, many: string) => `${count} ${count === 1 ? one : many}`;

/** A rainy day: every lesson of it cancelled at once, and each student told why. */
export function CancelDayForm({ date, today, onDateChange, affected }: CancelDayFormProps) {
  const cancelDay = useCancelDay();
  const done = cancelDay.data?.lessons.length;

  return (
    <div className="flex flex-col gap-4">
      <Field
        label="Día"
        type="date"
        min={today}
        value={date}
        required
        className="max-w-48"
        onChange={(event) => {
          cancelDay.reset();
          onDateChange(event.target.value);
        }}
      />
      {done !== undefined ? (
        <Notice>{done === 0 ? "No quedaba ninguna clase ese día." : `Día cancelado: ${plural(done, "clase", "clases")}.`}</Notice>
      ) : (
        <CancelConfirmation
          confirmLabel="Cancelar el día"
          pending={cancelDay.isPending}
          withReason
          disabled={!affected || affected.lessons === 0}
          onConfirm={(reason) => cancelDay.mutate({ date, reason })}
        >
          <p aria-live="polite">
            {!affected
              ? "Mirando las clases de ese día…"
              : affected.lessons === 0
                ? "Ese día no hay clases que cancelar."
                : `Se cancelarán ${plural(affected.lessons, "clase", "clases")} y ${plural(affected.bookings, "reserva", "reservas")}. ` +
                  "Cada alumno recibirá un correo."}
          </p>
        </CancelConfirmation>
      )}
      <ErrorNotice error={cancelDay.error} />
    </div>
  );
}
