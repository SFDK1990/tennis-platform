"use client";

import { addDays, formatShortDay, type LocalDate } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";

export function WeekNavigator({ from, to, onChange }: { from: LocalDate; to: LocalDate; onChange: (from: LocalDate) => void }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <Button variant="quiet" onClick={() => onChange(addDays(from, -7))} aria-label="Semana anterior" className="max-sm:px-3">
        <Icon name="chevronLeft" /><span className="max-sm:hidden">Anterior</span>
      </Button>
      <p className="text-center font-display text-lg font-semibold">
        {formatShortDay(from)} – {formatShortDay(to)}
      </p>
      <Button variant="quiet" onClick={() => onChange(addDays(from, 7))} aria-label="Semana siguiente" className="max-sm:px-3">
        <span className="max-sm:hidden">Siguiente</span><Icon name="chevronRight" />
      </Button>
    </div>
  );
}
