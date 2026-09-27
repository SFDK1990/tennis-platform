"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { StudentLessonActions } from "@/modules/booking/components/StudentLessonActions";
import { useCalendar, type CalendarLesson } from "@/modules/calendar/api";
import { LessonLine } from "@/modules/calendar/components/LessonLine";
import { LESSON_TITLE, seatsLeft } from "@/modules/calendar/labels";
import { studentHome } from "@/modules/calendar/select";
import { useMe, useResendVerification, type Me } from "@/modules/identity/api";
import { useTeacherProfile } from "@/modules/teacher/api";
import { addDays, dateIn, formatDay, formatTime } from "@/shared/time";
import { useNow } from "@/shared/useNow";
import { Button, BUTTON_BASE, BUTTON_VARIANTS } from "@/shared/ui/Button";
import { CourtCard } from "@/shared/ui/CourtCard";
import { EmptyState } from "@/shared/ui/EmptyState";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

/** Two weeks ahead: enough for "your next lesson" without paging through the calendar. */
const HORIZON_DAYS = 13;

export default function StudentHomePage() {
  const me = useMe().data;
  const now = useNow();
  // The range only needs to cover the teacher's days; the browser's zone is close enough for it.
  const today = dateIn(new Date(now), Intl.DateTimeFormat().resolvedOptions().timeZone);
  const calendar = useCalendar(today, addDays(today, HORIZON_DAYS));
  const teacher = useTeacherProfile().data?.displayName ?? "tu profesor";

  if (!me) {
    return null;
  }
  const managed = calendar.data ? calendar.data.timezone !== null : null;
  const zone = calendar.data?.timezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone;
  const home = calendar.data ? studentHome(calendar.data.lessons, now) : null;
  const firstName = me.fullName?.split(" ")[0];

  return (
    <>
      <div>
        <h1 className="font-display text-4xl font-semibold leading-none">{firstName ? `Hola, ${firstName}` : "Hola"}</h1>
        <p className="mt-1.5 text-muted first-letter:uppercase">{formatDay(today)}</p>
      </div>

      <FirstSteps me={me} managed={managed} teacher={teacher} />
      <ErrorNotice error={calendar.error} />

      {home && managed ? (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)] lg:items-start">
          {home.next ? (
            <NextLesson lesson={home.next} zone={zone} now={now} teacher={teacher} />
          ) : (
            <EmptyState title="Todavía no tienes clases reservadas"
              actions={<Link href="/calendar" className={`${BUTTON_BASE} ${BUTTON_VARIANTS.primary}`}>Ver las clases</Link>}>
              {home.open.length > 0
                ? "Elige una de las que tienen plazas y aparecerá aquí."
                : `${teacher} aún no ha abierto clases para estas dos semanas.`}
            </EmptyState>
          )}

          {home.open.length > 0 ? (
            <section aria-labelledby="open-lessons" className="flex flex-col gap-3">
              <div className="flex items-baseline justify-between gap-3">
                <h2 id="open-lessons" className="font-display text-2xl font-semibold">Con plazas estos días</h2>
                <Link href="/calendar" className="text-sm font-semibold text-court">Ver todas</Link>
              </div>
              <ul className="flex flex-col gap-2">
                {home.open.map((lesson) => (
                  <LessonLine key={lesson.id} lesson={lesson} zone={zone} withDay
                    actions={<StudentLessonActions lessonId={lesson.id} startsAt={lesson.startsAt}
                      lessonStatus={lesson.status} booking={lesson.myBooking} />} />
                ))}
              </ul>
            </section>
          ) : null}
        </div>
      ) : calendar.data ? null : (
        <p className="text-muted">Cargando…</p>
      )}
    </>
  );
}

function NextLesson({ lesson, zone, now, teacher }: { lesson: CalendarLesson; zone: string; now: number; teacher: string }) {
  const day = dateIn(lesson.startsAt, zone);
  const today = dateIn(new Date(now), zone);
  const when = day === today ? "Hoy" : day === addDays(today, 1) ? "Mañana" : formatDay(day);
  return (
    <CourtCard label="Tu próxima clase">
      <p className="text-sm font-semibold text-white/90">Tu próxima clase</p>
      <div>
        <p className="font-display text-5xl font-semibold leading-none first-letter:uppercase">
          {when}, {formatTime(lesson.startsAt, zone)}
        </p>
        <p className="mt-2">{LESSON_TITLE[lesson.type]} con {teacher} · hasta las {formatTime(lesson.endsAt, zone)}</p>
      </div>
      <p className="text-sm text-white/90">{lesson.capacity > 1 ? seatsLeft(lesson) : "Clase para ti solo"}</p>
      <div className="self-start rounded-lg bg-paper p-1.5 text-ink">
        <StudentLessonActions lessonId={lesson.id} startsAt={lesson.startsAt} lessonStatus={lesson.status}
          booking={lesson.myBooking} />
      </div>
    </CourtCard>
  );
}

/**
 * What stands between this account and booking, in the order it has to happen: the states
 * AccountNotices shows elsewhere, as a checklist (27-fase15.5, decision 6).
 */
function FirstSteps({ me, managed, teacher }: { me: Me; managed: boolean | null; teacher: string }) {
  const resend = useResendVerification();
  const verified = me.status !== "PENDING_VERIFICATION";
  const profiled = me.fullName !== null;
  if (verified && profiled && managed !== false) {
    return null;
  }
  return (
    <section aria-labelledby="first-steps" className="rounded-xl border border-line bg-paper px-4 py-2 sm:px-5">
      <h2 id="first-steps" className="pb-1 pt-3 font-display text-2xl font-semibold">Tres pasos para reservar</h2>
      <ol>
        <Step number={1} done={verified} title="Confirma tu email"
          action={verified ? null : resend.isSuccess ? (
            <span className="text-sm text-muted">Enviado a {me.email}</span>
          ) : (
            <Button variant="quiet" pending={resend.isPending} onClick={() => resend.mutate()}>Reenviar correo</Button>
          )}>
          Abre el enlace que te enviamos a {me.email}.
        </Step>
        <Step number={2} done={profiled} title="Completa tu perfil"
          action={profiled ? null : <Link href="/profile" className={`${BUTTON_BASE} ${BUTTON_VARIANTS.primary}`}>Completar</Link>}>
          {teacher} necesita tu nombre para añadirte.
        </Step>
        <Step number={3} done={managed === true} title={`Espera a que ${teacher} te añada`} last>
          En cuanto lo haga, verás sus clases aquí y podrás reservar.
        </Step>
      </ol>
      <ErrorNotice error={resend.error} />
    </section>
  );
}

function Step({ number, done, title, action, last, children }: {
  number: number; done: boolean; title: string; action?: ReactNode; last?: boolean; children: ReactNode;
}) {
  return (
    <li className={`flex flex-wrap items-center gap-x-4 gap-y-2 py-3 ${last ? "" : "border-b border-line-soft"}`}>
      {done ? (
        <span className="flex size-8 items-center justify-center rounded-full bg-surround text-white">
          <Icon name="check" className="size-4" />
          <span className="sr-only">Hecho:</span>
        </span>
      ) : (
        <span className="flex size-8 items-center justify-center rounded-full border-2 border-court font-display text-lg font-bold text-court">
          {number}
        </span>
      )}
      <div className="min-w-0 flex-1">
        <p className={`font-semibold ${done ? "text-muted line-through" : ""}`}>{title}</p>
        {done ? null : <p className="text-sm text-muted">{children}</p>}
      </div>
      {action}
    </li>
  );
}
