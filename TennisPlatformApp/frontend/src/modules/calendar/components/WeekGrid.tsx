"use client";

import Link from "next/link";
import type { Calendar, CalendarLesson } from "@/modules/calendar/api";
import { hourRange, ROW_MINUTES, rowsOf } from "@/modules/calendar/grid";
import { seatsLeft } from "@/modules/calendar/labels";
import { addDays, dateIn, formatDay, formatShortWeekday, formatTime, minutesIn, type LocalDate } from "@/shared/time";

const SHORT_TITLE: Record<CalendarLesson["type"], string> = { INDIVIDUAL: "Individual", GROUP: "Grupo" };

const LOOK: Record<CalendarLesson["status"], string> = {
  OPEN: "",
  FULL: "",
  CANCELLED: "!border-line !bg-line-soft !text-muted line-through",
  COMPLETED: "opacity-70",
};

interface WeekGridProps {
  calendar: Calendar;
  from: LocalDate;
  now: number;
  hrefFor: (lesson: CalendarLesson) => string;
}

/**
 * The desk view of the agenda (27-fase15.5, decision 7): seven columns of the teacher's wall
 * clock, the hours they are available in white over a hatched day, and the lessons as blocks.
 * Blocks are placed on CSS grid rows by class, never by inline style (see globals.css).
 */
export function WeekGrid({ calendar, from, now, hrefFor }: WeekGridProps) {
  const zone = calendar.timezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone;
  const days = Array.from({ length: 7 }, (_, offset) => addDays(from, offset));
  const { first, last } = hourRange([...calendar.lessons, ...calendar.availability], zone);
  const hours = Array.from({ length: last - first }, (_, i) => first + i);
  const today = dateIn(new Date(now), zone);
  const nowRow = Math.floor((minutesIn(new Date(now).toISOString(), zone) - first * 60) / ROW_MINUTES) + 1;

  return (
    <div className="overflow-x-auto rounded-xl border border-line bg-paper">
      <div className="min-w-[52rem]">
        <div aria-hidden className="flex border-b border-line">
          <span className="w-14 shrink-0" />
          <div className="grid flex-1 grid-cols-7">
            {days.map((day) => (
              <p key={day}
                className={`border-l border-line px-3 py-2.5 font-display text-lg font-semibold first-letter:uppercase ${day === today ? "bg-court text-white" : ""}`}>
                {formatShortWeekday(day)}{day === today ? " · hoy" : ""}
              </p>
            ))}
          </div>
        </div>

        <div className="flex">
          <div aria-hidden className="grid w-14 shrink-0 auto-rows-[12px]">
            {hours.map((hour, i) => (
              <span key={hour} className={`${rowClass(i * 4 + 1, 4)} -translate-y-2 pr-2 text-right text-xs text-muted`}>
                {i === 0 ? "" : `${hour}:00`}
              </span>
            ))}
          </div>
          <ol className="grid flex-1 grid-cols-7">
            {days.map((day) => {
              const lessons = calendar.lessons.filter((lesson) => dateIn(lesson.startsAt, zone) === day);
              const hoursOpen = calendar.availability.filter((interval) => dateIn(interval.startsAt, zone) === day);
              return (
                <li key={day}
                  className="relative border-l border-line bg-[repeating-linear-gradient(135deg,var(--color-ground)_0_6px,var(--color-line-soft)_6px_12px)]">
                  <h3 className="sr-only">{formatDay(day)}</h3>
                  <div aria-hidden className="absolute inset-0 grid auto-rows-[12px]">
                    {hoursOpen.map((interval) => {
                      const { start, span } = rowsOf(interval, zone, first);
                      return <span key={interval.startsAt} className={`${rowClass(start, span)} col-start-1 bg-paper`} />;
                    })}
                  </div>
                  <div aria-hidden
                    className="pointer-events-none absolute inset-0 bg-[repeating-linear-gradient(to_bottom,transparent_0_47px,var(--color-line-soft)_47px_48px)]" />
                  {/* Sized by the hour labels: the column is as tall as the day it shows. */}
                  <div className="invisible grid auto-rows-[12px]">
                    <span className={rowClass(1, hours.length * 4)} />
                  </div>
                  <ul className="absolute inset-0 grid auto-rows-[12px]">
                    {lessons.map((lesson) => {
                      const { start, span } = rowsOf(lesson, zone, first);
                      return (
                        // A cancelled lesson stays visible but goes under the live ones it may overlap.
                        <li key={lesson.id} className={`${rowClass(start, span)} col-start-1 p-0.5 ${lesson.status === "CANCELLED" ? "z-0 opacity-70" : "z-10"}`}>
                          <Link href={hrefFor(lesson)}
                            className={`flex h-full flex-col overflow-hidden rounded-md px-2 py-1 text-xs leading-tight hover:ring-2 hover:ring-navy ${
                              lesson.type === "GROUP" ? "bg-court text-white" : "border-[1.5px] border-court bg-paper text-ink"
                            } ${LOOK[lesson.status]}`}>
                            <span className="font-semibold">
                              {formatTime(lesson.startsAt, zone)} {SHORT_TITLE[lesson.type]}
                            </span>
                            <span>{lesson.status === "CANCELLED" ? "Cancelada" : seatsLeft(lesson)}</span>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                  {day === today && nowRow >= 1 && nowRow <= hours.length * 4 ? (
                    <div aria-hidden className="pointer-events-none absolute inset-0 z-20 grid auto-rows-[12px]">
                      <span className={`${rowClass(nowRow, 1)} col-start-1 border-t-2 border-fault`} />
                    </div>
                  ) : null}
                </li>
              );
            })}
          </ol>
        </div>
      </div>
    </div>
  );
}

/** Literal names for the classes globals.css generates, so the scanner and the grid agree. */
function rowClass(start: number, span: number): string {
  return `row-start-${start} row-span-${span}`;
}
