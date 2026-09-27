"use client";

import Link from "next/link";
import { LessonAttendance } from "@/modules/booking/components/LessonAttendance";
import { useCalendar, type CalendarLesson } from "@/modules/calendar/api";
import { LessonLine } from "@/modules/calendar/components/LessonLine";
import { LESSON_TITLE } from "@/modules/calendar/labels";
import { teacherDay } from "@/modules/calendar/select";
import { useStudentNames } from "@/modules/student/api";
import { useTeacherZone } from "@/modules/teacher/api";
import { addDays, dateIn, formatDay, formatTime } from "@/shared/time";
import { useNow } from "@/shared/useNow";
import { BUTTON_BASE, BUTTON_VARIANTS } from "@/shared/ui/Button";
import { CourtCard } from "@/shared/ui/CourtCard";
import { EmptyState } from "@/shared/ui/EmptyState";
import { ErrorNotice } from "@/shared/ui/Notice";

/** The teacher's first screen: the lesson on court or about to start, and who is in it. */
export default function TodayPage() {
  const zone = useTeacherZone();
  return zone ? <Today zone={zone} /> : <p className="text-muted">Cargando…</p>;
}

function Today({ zone }: { zone: string }) {
  const now = useNow();
  const today = dateIn(new Date(now), zone);
  const calendar = useCalendar(today, addDays(today, 6));
  const nameOf = useStudentNames();
  const day = calendar.data ? teacherDay(calendar.data.lessons, zone, today, now) : null;
  const count = day ? day.rest.length + (day.focus ? 1 : 0) : 0;

  return (
    <>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-4xl font-semibold leading-none">Hoy</h1>
          <p className="mt-1.5 text-muted first-letter:uppercase">
            {formatDay(today)}
            {day ? ` · ${count === 0 ? "sin clases" : count === 1 ? "1 clase" : `${count} clases`}` : ""}
          </p>
        </div>
        {/* On a phone the tab bar already leads there. */}
        <Link href="/teacher/calendar" className={`${BUTTON_BASE} ${BUTTON_VARIANTS.secondary} max-lg:hidden`}>Ver la agenda</Link>
      </div>

      <ErrorNotice error={calendar.error} />
      {!day ? (
        <p className="text-muted">Cargando…</p>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)] lg:items-start">
          <div className="flex flex-col gap-4">
            {day.focus ? (
              <>
                <Focus lesson={day.focus} zone={zone} now={now} />
                <section aria-labelledby="students" className="flex flex-col gap-3">
                  <h2 id="students" className="font-display text-2xl font-semibold">Alumnos</h2>
                  <LessonAttendance key={day.focus.id} lessonId={day.focus.id} nameOf={nameOf}
                    started={new Date(day.focus.startsAt).getTime() <= now} />
                </section>
              </>
            ) : (
              <EmptyState
                title={day.rest.length > 0 ? "Has terminado por hoy" : "Hoy no tienes clases"}
                actions={
                  <Link href="/teacher/calendar" className={`${BUTTON_BASE} ${BUTTON_VARIANTS.primary}`}>
                    Abrir la agenda
                  </Link>
                }
              >
                {day.upcoming.length > 0
                  ? "Aquí al lado tienes las próximas. Desde la agenda puedes crear más."
                  : "Esta semana no hay nada más. Crea una clase desde la agenda y tus alumnos la verán al momento."}
              </EmptyState>
            )}
          </div>

          <div className="flex flex-col gap-6">
            {day.rest.length > 0 ? (
              <LessonList title="Más clases de hoy" lessons={day.rest} zone={zone} />
            ) : null}
            {day.upcoming.length > 0 ? (
              <LessonList title="Próximos días" lessons={day.upcoming} zone={zone} withDay />
            ) : null}
          </div>
        </div>
      )}
    </>
  );
}

function Focus({ lesson, zone, now }: { lesson: CalendarLesson; zone: string; now: number }) {
  const started = new Date(lesson.startsAt).getTime() <= now;
  return (
    <CourtCard label={started ? "Clase en curso" : "Próxima clase"}>
      <div className="flex items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-white/90">
            {started ? `En pista hasta las ${formatTime(lesson.endsAt, zone)}` : "Próxima clase"}
          </p>
          <p className="mt-2 whitespace-nowrap font-display text-4xl font-semibold leading-none sm:text-5xl">
            {formatTime(lesson.startsAt, zone)}–{formatTime(lesson.endsAt, zone)}
          </p>
          <p className="mt-2">{LESSON_TITLE[lesson.type]}</p>
        </div>
        <p className="w-[30%] shrink-0 text-center font-display leading-none">
          <span className="block text-4xl font-semibold">{lesson.bookedCount}/{lesson.capacity}</span>
          <span className="text-sm font-medium">{lesson.capacity === 1 ? "plaza" : "plazas"}</span>
        </p>
      </div>
      <Link href={`/teacher/lessons/${lesson.id}`} className={`${BUTTON_BASE} ${BUTTON_VARIANTS.onCourt} self-start`}>
        Ver clase
      </Link>
    </CourtCard>
  );
}

function LessonList({ title, lessons, zone, withDay }: { title: string; lessons: CalendarLesson[]; zone: string; withDay?: boolean }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="font-display text-2xl font-semibold">{title}</h2>
      <ul className="flex flex-col gap-2">
        {lessons.map((lesson) => (
          <LessonLine key={lesson.id} lesson={lesson} zone={zone} withDay={withDay} href={`/teacher/lessons/${lesson.id}`} />
        ))}
      </ul>
    </section>
  );
}
