"use client";

import type { ReactNode } from "react";
import type { Calendar, CalendarLesson } from "@/modules/calendar/api";
import { addDays, dateIn, formatDay, formatTime, type LocalDate } from "@/shared/time";

const LESSON_TYPE: Record<CalendarLesson["type"], string> = { INDIVIDUAL: "Individual", GROUP: "Grupo" };
const LESSON_STATUS: Partial<Record<CalendarLesson["status"], string>> = {
  FULL: "Completa",
  CANCELLED: "Cancelada",
  COMPLETED: "Terminada",
};

interface WeekCalendarProps {
  calendar: Calendar;
  from: LocalDate;
  /** What can be done with a lesson; the calendar only shows it, other modules act on it. */
  renderActions?: (lesson: CalendarLesson) => ReactNode;
  emptyDay?: string;
}

export function WeekCalendar({ calendar, from, renderActions, emptyDay = "Sin clases" }: WeekCalendarProps) {
  const zone = calendar.timezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone;
  const days = Array.from({ length: 7 }, (_, offset) => addDays(from, offset));
  const lessonsByDay = groupBy(calendar.lessons, (lesson) => dateIn(lesson.startsAt, zone));
  const hoursByDay = groupBy(calendar.availability, (interval) => dateIn(interval.startsAt, zone));

  return (
    <div>
      <p className="mb-2 text-sm text-muted">Horas de {zone.split("/").pop()?.replaceAll("_", " ")}</p>
      <ol className="flex flex-col">
      {days.map((day) => {
        const lessons = lessonsByDay.get(day) ?? [];
        const hours = hoursByDay.get(day) ?? [];
        return (
          <li key={day} className="border-t border-line py-4">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <h3 className="font-display text-xl font-semibold first-letter:uppercase">{formatDay(day)}</h3>
              {hours.length > 0 ? (
                <p className="text-sm text-muted">
                  Disponible {hours.map((h) => `${formatTime(h.startsAt, zone)}–${formatTime(h.endsAt, zone)}`).join(", ")}
                </p>
              ) : null}
            </div>
            {lessons.length === 0 ? (
              <p className="mt-2 text-sm text-muted">{emptyDay}</p>
            ) : (
              <ul className="mt-3 flex flex-col gap-2">
                {lessons.map((lesson) => (
                  <LessonRow key={lesson.id} lesson={lesson} zone={zone} actions={renderActions?.(lesson)} />
                ))}
              </ul>
            )}
          </li>
        );
      })}
      </ol>
    </div>
  );
}

function LessonRow({ lesson, zone, actions }: { lesson: CalendarLesson; zone: string; actions: ReactNode }) {
  const inactive = lesson.status === "CANCELLED" || lesson.status === "COMPLETED";
  const statusLabel = LESSON_STATUS[lesson.status];
  return (
    <li className={`flex flex-wrap items-center gap-x-5 gap-y-3 rounded-xl border border-line bg-paper px-4 py-3 ${inactive ? "opacity-60" : ""}`}>
      <p className="w-16 font-display leading-tight">
        <span className="block text-2xl font-semibold">{formatTime(lesson.startsAt, zone)}</span>
        <span className="text-sm text-muted">{formatTime(lesson.endsAt, zone)}</span>
      </p>
      <div className="flex min-w-32 flex-1 flex-col gap-1">
        <p className="font-semibold">
          {LESSON_TYPE[lesson.type]}
          {statusLabel ? <span className="ml-2 text-sm font-normal text-muted">{statusLabel}</span> : null}
        </p>
        <Seats capacity={lesson.capacity} taken={lesson.bookedCount} muted={inactive} />
      </div>
      {actions ? <div className="flex items-center gap-2">{actions}</div> : null}
    </li>
  );
}

/** One dot per seat: filled when taken, court blue while it can still be taken. */
function Seats({ capacity, taken, muted }: { capacity: number; taken: number; muted: boolean }) {
  const free = Math.max(capacity - taken, 0);
  return (
    <p className="flex items-center gap-2 text-sm text-muted">
      <span aria-hidden className="flex gap-1">
        {Array.from({ length: capacity }, (_, seat) => (
          <span
            key={seat}
            className={`size-3 rounded-full border ${
              seat < taken ? "border-navy bg-navy" : muted ? "border-line bg-paper" : "border-court bg-court-tint"
            }`}
          />
        ))}
      </span>
      {free === 0 ? "Sin plazas" : free === 1 ? "1 plaza libre" : `${free} plazas libres`}
    </p>
  );
}

function groupBy<T>(items: T[], keyOf: (item: T) => string): Map<string, T[]> {
  const groups = new Map<string, T[]>();
  for (const item of items) {
    const key = keyOf(item);
    groups.set(key, [...(groups.get(key) ?? []), item]);
  }
  return groups;
}
