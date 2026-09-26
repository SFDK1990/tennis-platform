"use client";

import { addDays, formatShortDay, type LocalDate } from "@/shared/time";
import { Button } from "@/shared/ui/Button";

export function WeekNavigator({ from, to, onChange }: { from: LocalDate; to: LocalDate; onChange: (from: LocalDate) => void }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <Button variant="quiet" onClick={() => onChange(addDays(from, -7))} aria-label="Semana anterior">
        Anterior
      </Button>
      <p className="text-center font-display text-lg font-semibold">
        {formatShortDay(from)} – {formatShortDay(to)}
      </p>
      <Button variant="quiet" onClick={() => onChange(addDays(from, 7))} aria-label="Semana siguiente">
        Siguiente
      </Button>
    </div>
  );
}
