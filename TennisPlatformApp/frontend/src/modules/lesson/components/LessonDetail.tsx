"use client";

import { useState } from "react";
import { useCancelFollowing, useCancelLesson, useChangeLessonCourt, useChangeLessonLevel, type Lesson } from "@/modules/lesson/api";
import { CancelConfirmation } from "@/modules/lesson/components/CancelConfirmation";
import { EditLessonForm } from "@/modules/lesson/components/EditLessonForm";
import { CourtSelect, useCourtName } from "@/shared/courts";
import { LevelSelect, useLevelName } from "@/shared/levels";
import { dateIn, formatDay, formatTime } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { CourtCard } from "@/shared/ui/CourtCard";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

const STATUS: Record<Lesson["status"], string> = {
  OPEN: "Abierta",
  FULL: "Completa",
  CANCELLED: "Cancelada",
  COMPLETED: "Terminada",
};

interface LessonDetailProps {
  lesson: Lesson;
  zone: string;
  /** Once it has started, what the lesson is can no longer change. */
  started: boolean;
  /** The administrator cancels through their own route and does not edit. */
  as?: "teacher" | "admin";
}

export function LessonDetail({ lesson, zone, started, as = "teacher" }: LessonDetailProps) {
  const cancel = useCancelLesson(as);
  const cancelFollowing = useCancelFollowing();
  const changeLevel = useChangeLessonLevel(lesson.id);
  const levelName = useLevelName();
  const changeCourt = useChangeLessonCourt(lesson.id);
  const courtName = useCourtName();
  // Which cancellation is being confirmed: this lesson, or this one and the rest of its series.
  const [confirming, setConfirming] = useState<"lesson" | "following" | null>(null);
  const [editing, setEditing] = useState(false);
  const cancellable = lesson.status === "OPEN" || lesson.status === "FULL";
  const editable = as === "teacher" && cancellable && !started;

  return (
    <section className="flex flex-col gap-3">
      <CourtCard label="La clase" tone="navy">
        <h1 className="flex flex-col gap-2">
          <span className="text-sm font-semibold text-white/85 first-letter:uppercase">{formatDay(dateIn(lesson.startsAt, zone))}</span>
          <span className="font-display text-5xl font-semibold leading-none">
            {formatTime(lesson.startsAt, zone)}–{formatTime(lesson.endsAt, zone)}
          </span>
        </h1>
        <p>
          {lesson.type === "INDIVIDUAL" ? "Clase individual" : `Clase de grupo, ${lesson.capacity} plazas`}.{" "}
          {STATUS[lesson.status]}, con {lesson.bookedCount} {lesson.bookedCount === 1 ? "reserva" : "reservas"}.
        </p>
        <p className="text-sm text-white/85">
          {courtName(lesson.courtId) ? `${courtName(lesson.courtId)}. ` : ""}
          {levelName(lesson.levelId) ? `Nivel ${levelName(lesson.levelId)}` : "Abierta a todos los niveles"}.
        </p>
        {lesson.seriesPosition ? (
          <p className="text-sm text-white/85">Se repite cada semana: es la {lesson.seriesPosition} de {lesson.seriesSize}.</p>
        ) : null}
      </CourtCard>
      {lesson.notes && !editing ? <p className="rounded-xl border border-line bg-paper px-4 py-3">{lesson.notes}</p> : null}
      {lesson.cancelledAtShortNotice ? (
        <Notice>Se canceló con menos de 24 horas de aviso.</Notice>
      ) : null}
      {editing ? <EditLessonForm lesson={lesson} onDone={() => setEditing(false)} /> : null}
      {editable ? (
        <div className="flex flex-wrap gap-4">
          <div className="w-64">
            <LevelSelect id="lesson-detail-level" label="Nivel de la clase" noneLabel="Abierta a todos" value={lesson.levelId}
              onChange={(levelId) => changeLevel.mutate(levelId)} />
          </div>
          <div className="w-48">
            <CourtSelect id="lesson-detail-court" label="Pista" value={lesson.courtId}
              onChange={(courtId) => changeCourt.mutate(courtId)} />
          </div>
        </div>
      ) : null}
      {editable && lesson.bookedCount > 0 ? (
        <p className="text-sm text-muted">Si cambias la pista, los alumnos con plaza reciben un correo.</p>
      ) : null}
      {changeCourt.isSuccess ? <Notice tone="success">Pista cambiada.</Notice> : null}
      <ErrorNotice error={changeLevel.error ?? changeCourt.error} />
      <ErrorNotice error={cancel.error ?? cancelFollowing.error} />
      {cancellable && !editing ? (
        confirming === "lesson" ? (
          <CancelConfirmation
            confirmLabel="Sí, cancelar la clase"
            pending={cancel.isPending}
            withReason={as === "teacher"}
            onConfirm={(reason) => cancel.mutate({ id: lesson.id, reason })}
            onBack={() => setConfirming(null)}
          >
            <p>Se cancelarán también todas sus reservas, y cada alumno recibirá un correo.</p>
          </CancelConfirmation>
        ) : confirming === "following" ? (
          <CancelConfirmation
            confirmLabel="Sí, cancelar esta y las siguientes"
            pending={cancelFollowing.isPending}
            withReason
            onConfirm={(reason) => cancelFollowing.mutate({ id: lesson.id, reason })}
            onBack={() => setConfirming(null)}
          >
            <p>
              Se cancelarán esta clase y las que vienen después en la serie, con sus reservas, y cada alumno recibirá un
              correo. Las anteriores no cambian.
            </p>
          </CancelConfirmation>
        ) : (
          <div className="flex flex-wrap gap-2">
            {editable ? <Button variant="quiet" onClick={() => setEditing(true)}>Editar</Button> : null}
            <Button variant="danger" onClick={() => setConfirming("lesson")}>Cancelar clase</Button>
            {as === "teacher" && lesson.seriesPosition && lesson.seriesSize && lesson.seriesPosition < lesson.seriesSize ? (
              <Button variant="danger" onClick={() => setConfirming("following")}>Cancelar esta y las siguientes</Button>
            ) : null}
          </div>
        )
      ) : null}
    </section>
  );
}
