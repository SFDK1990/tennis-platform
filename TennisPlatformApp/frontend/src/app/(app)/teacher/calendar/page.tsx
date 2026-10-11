"use client";

import { useState } from "react";
import { AgendaWeek } from "@/modules/calendar/components/AgendaWeek";
import { CreateLessonForm } from "@/modules/lesson/components/CreateLessonForm";
import { useTeacherZone } from "@/modules/teacher/api";
import { todayIn } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";

const hrefFor = (lesson: { id: string }) => `/teacher/lessons/${lesson.id}`;

export default function TeacherCalendarPage() {
  const zone = useTeacherZone() ?? "Europe/Madrid";
  const [creating, setCreating] = useState(false);

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
      <AgendaWeek zone={zone} hrefFor={hrefFor} />
    </>
  );
}
