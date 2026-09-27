"use client";

import { useAvailability } from "@/modules/availability/api";
import { ExceptionsEditor } from "@/modules/availability/components/ExceptionsEditor";
import { WeeklyRulesEditor } from "@/modules/availability/components/WeeklyRulesEditor";
import { useTeacherZone } from "@/modules/teacher/api";
import { addDays, todayIn } from "@/shared/time";
import { ErrorNotice } from "@/shared/ui/Notice";

export default function AvailabilityPage() {
  const zone = useTeacherZone() ?? "Europe/Madrid";
  const today = todayIn(zone);
  // The widest range the backend accepts: the next two months of exceptions.
  const availability = useAvailability(today, addDays(today, 61));

  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Horario</h1>
      <ErrorNotice error={availability.error} />
      {availability.data ? (
        <div className="flex max-w-3xl flex-col gap-6">
          <section className="flex flex-col gap-3">
            <h2 className="font-display text-2xl font-semibold">Cada semana</h2>
            <WeeklyRulesEditor rules={availability.data.weeklyRules} />
          </section>
          <section className="flex flex-col gap-3">
            <h2 className="font-display text-2xl font-semibold">Días concretos</h2>
            <ExceptionsEditor exceptions={availability.data.exceptions} today={today} />
          </section>
        </div>
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </>
  );
}
