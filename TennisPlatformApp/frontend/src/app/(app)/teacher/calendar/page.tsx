"use client";

import Link from "next/link";
import { useState } from "react";
import { useCalendar } from "@/modules/calendar/api";
import { WeekCalendar } from "@/modules/calendar/components/WeekCalendar";
import { WeekGrid } from "@/modules/calendar/components/WeekGrid";
import { WeekNavigator } from "@/modules/calendar/components/WeekNavigator";
import { CreateLessonForm } from "@/modules/lesson/components/CreateLessonForm";
import { useTeacherZone } from "@/modules/teacher/api";
import { todayIn, weekOf } from "@/shared/time";
import { useNow } from "@/shared/useNow";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

export default function TeacherCalendarPage() {
  const zone = useTeacherZone() ?? "Europe/Madrid";
  const [from, setFrom] = useState(() => weekOf(todayIn(zone)).from);
  const { to } = weekOf(from);
  const calendar = useCalendar(from, to);
  const now = useNow();
  const [creating, setCreating] = useState(false);
  const hrefFor = (lesson: { id: string }) => `/teacher/lessons/${lesson.id}`;

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-4xl font-semibold leading-none">Agenda</h1>
        <Button variant={creating ? "quiet" : "primary"} onClick={() => setCreating(!creating)}>
          {creating ? "Cerrar" : <><Icon name="plus" />Nueva clase</>}
        </Button>
      </div>
      {creating ? (
        <section aria-label="Nueva clase" className="rounded-xl border border-line bg-paper p-5">
          <CreateLessonForm zone={zone} defaultDate={todayIn(zone)} />
        </section>
      ) : null}
      <WeekNavigator from={from} to={to} onChange={setFrom} />
      <ErrorNotice error={calendar.error} />
      {calendar.data ? (
        <>
          {/* Only one of the two is ever displayed, so the page never lists a day twice. */}
          <div className="lg:hidden">
            <WeekCalendar
              calendar={calendar.data}
              from={from}
              renderActions={(lesson) => (
                <Link href={hrefFor(lesson)} className="rounded-lg border border-line px-3 py-2 text-sm font-semibold hover:border-court">
                  Ver clase
                </Link>
              )}
            />
          </div>
          <div className="flex flex-col gap-3 max-lg:hidden">
            <WeekGrid calendar={calendar.data} from={from} now={now} hrefFor={hrefFor} />
            <Legend />
          </div>
        </>
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </>
  );
}

function Legend() {
  return (
    <p className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted">
      <span className="flex items-center gap-2"><i className="size-3.5 rounded-sm bg-court" />Grupo</span>
      <span className="flex items-center gap-2"><i className="size-3.5 rounded-sm border-[1.5px] border-court bg-paper" />Individual</span>
      <span className="flex items-center gap-2"><i className="size-3.5 rounded-sm bg-paper ring-1 ring-line" />Disponible</span>
      <span className="flex items-center gap-2">
        <i className="size-3.5 rounded-sm bg-[repeating-linear-gradient(135deg,var(--color-ground)_0_3px,var(--color-line)_3px_6px)]" />
        Sin disponibilidad
      </span>
      <span className="flex items-center gap-2"><i className="h-0.5 w-3.5 bg-fault" />Ahora</span>
    </p>
  );
}
