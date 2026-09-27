"use client";

import Link from "next/link";
import { useState } from "react";
import { useCalendar } from "@/modules/calendar/api";
import { WeekCalendar } from "@/modules/calendar/components/WeekCalendar";
import { WeekNavigator } from "@/modules/calendar/components/WeekNavigator";
import { CreateLessonForm } from "@/modules/lesson/components/CreateLessonForm";
import { useTeacherZone } from "@/modules/teacher/api";
import { todayIn, weekOf } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/Notice";

export default function TeacherCalendarPage() {
  const zone = useTeacherZone() ?? "Europe/Madrid";
  const [from, setFrom] = useState(() => weekOf(todayIn(zone)).from);
  const { to } = weekOf(from);
  const calendar = useCalendar(from, to);
  const [creating, setCreating] = useState(false);

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-3xl font-semibold">Calendario</h1>
        <Button variant={creating ? "quiet" : "primary"} onClick={() => setCreating(!creating)}>
          {creating ? "Cerrar" : "Nueva clase"}
        </Button>
      </div>
      {creating ? (
        <section className="rounded-md border border-line bg-paper p-4">
          <CreateLessonForm zone={zone} defaultDate={todayIn(zone)} />
        </section>
      ) : null}
      <WeekNavigator from={from} to={to} onChange={setFrom} />
      <ErrorNotice error={calendar.error} />
      {calendar.data ? (
        <WeekCalendar
          calendar={calendar.data}
          from={from}
          renderActions={(lesson) => (
            <Link href={`/teacher/lessons/${lesson.id}`} className="rounded-md border border-line px-3 py-2 text-sm font-semibold hover:border-court">
              Ver clase
            </Link>
          )}
        />
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </>
  );
}
