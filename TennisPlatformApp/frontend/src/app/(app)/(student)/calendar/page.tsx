"use client";

import { useState } from "react";
import { StudentLessonActions } from "@/modules/booking/components/StudentLessonActions";
import { useMe } from "@/modules/identity/api";
import { useCalendar } from "@/modules/calendar/api";
import { WeekCalendar } from "@/modules/calendar/components/WeekCalendar";
import { WeekNavigator } from "@/modules/calendar/components/WeekNavigator";
import { todayIn, weekOf } from "@/shared/time";
import { EmptyState } from "@/shared/ui/EmptyState";
import { ErrorNotice } from "@/shared/ui/Notice";

export default function StudentCalendarPage() {
  const [from, setFrom] = useState(() => weekOf(todayIn(Intl.DateTimeFormat().resolvedOptions().timeZone)).from);
  const { to } = weekOf(from);
  const calendar = useCalendar(from, to);
  const myLevelId = useMe().data?.levelId ?? null;

  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Clases</h1>
      <WeekNavigator from={from} to={to} onChange={setFrom} />
      <ErrorNotice error={calendar.error} />
      {calendar.data?.timezone === null ? (
        <EmptyState title="Aún no ves ninguna clase">
          Cuando tu profesor te añada a sus alumnos, aquí verás sus clases y podrás reservar.
        </EmptyState>
      ) : calendar.data ? (
        <WeekCalendar
          calendar={calendar.data}
          from={from}
          renderActions={(lesson) => (
            <StudentLessonActions lessonId={lesson.id} startsAt={lesson.startsAt} lessonStatus={lesson.status}
              booking={lesson.myBooking} levelId={lesson.levelId} myLevelId={myLevelId} />
          )}
        />
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </>
  );
}
