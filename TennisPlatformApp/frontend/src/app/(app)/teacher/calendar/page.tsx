"use client";

import { useState } from "react";
import { useCalendar } from "@/modules/calendar/api";
import { AgendaWeek } from "@/modules/calendar/components/AgendaWeek";
import { CancelDayForm } from "@/modules/lesson/components/CancelDayForm";
import { CreateLessonForm } from "@/modules/lesson/components/CreateLessonForm";
import { useTeacherZone } from "@/modules/teacher/api";
import { dateIn, todayIn } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";
import { useNow } from "@/shared/useNow";

const hrefFor = (lesson: { id: string }) => `/teacher/lessons/${lesson.id}`;

type Panel = "create" | "cancel-day" | null;

export default function TeacherCalendarPage() {
  const zone = useTeacherZone() ?? "Europe/Madrid";
  const [panel, setPanel] = useState<Panel>(null);
  const toggle = (next: Panel) => setPanel(panel === next ? null : next);

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-4xl font-semibold leading-none">Agenda</h1>
        <div className="flex flex-wrap gap-2">
          <Button variant="quiet" onClick={() => toggle("cancel-day")}>
            {panel === "cancel-day" ? "Cerrar" : "Cancelar un día"}
          </Button>
          <Button variant={panel === "create" ? "quiet" : "primary"} onClick={() => toggle("create")}>
            {panel === "create" ? "Cerrar" : <><Icon name="plus" />Nueva clase</>}
          </Button>
        </div>
      </div>
      {panel === "create" ? (
        <section aria-label="Nueva clase" className="rounded-xl border border-line bg-paper p-5">
          <CreateLessonForm zone={zone} defaultDate={todayIn(zone)} />
        </section>
      ) : null}
      {panel === "cancel-day" ? (
        <section aria-label="Cancelar un día" className="rounded-xl border border-line bg-paper p-5">
          <CancelDay zone={zone} />
        </section>
      ) : null}
      <AgendaWeek zone={zone} hrefFor={hrefFor} />
    </>
  );
}

/** Joins the calendar, which knows what a day holds, and the form of the lesson module. */
function CancelDay({ zone }: { zone: string }) {
  const today = todayIn(zone);
  const [date, setDate] = useState(today);
  const calendar = useCalendar(date, date);
  const now = useNow();
  const lessons = calendar.data && !calendar.isPlaceholderData
    ? calendar.data.lessons.filter((lesson) =>
        dateIn(lesson.startsAt, zone) === date
        && (lesson.status === "OPEN" || lesson.status === "FULL")
        && new Date(lesson.startsAt).getTime() > now)
    : undefined;

  return (
    <CancelDayForm
      date={date}
      today={today}
      onDateChange={setDate}
      affected={lessons && {
        lessons: lessons.length,
        bookings: lessons.reduce((sum, lesson) => sum + lesson.bookedCount, 0),
      }}
    />
  );
}
