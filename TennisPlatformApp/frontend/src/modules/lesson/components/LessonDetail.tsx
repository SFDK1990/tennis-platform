"use client";

import { useState } from "react";
import { useCancelLesson, type Lesson } from "@/modules/lesson/api";
import { EditLessonForm } from "@/modules/lesson/components/EditLessonForm";
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
  const [confirming, setConfirming] = useState(false);
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
      </CourtCard>
      {lesson.notes && !editing ? <p className="rounded-xl border border-line bg-paper px-4 py-3">{lesson.notes}</p> : null}
      {lesson.cancelledAtShortNotice ? (
        <Notice>Se canceló con menos de 24 horas de aviso.</Notice>
      ) : null}
      {editing ? <EditLessonForm lesson={lesson} onDone={() => setEditing(false)} /> : null}
      <ErrorNotice error={cancel.error} />
      {cancellable && !editing ? (
        confirming ? (
          <div className="flex flex-wrap items-center gap-2">
            <span>Se cancelarán también todas sus reservas.</span>
            <Button variant="danger" pending={cancel.isPending} onClick={() => cancel.mutate(lesson.id)}>
              Sí, cancelar la clase
            </Button>
            <Button variant="quiet" onClick={() => setConfirming(false)}>No</Button>
          </div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {editable ? <Button variant="quiet" onClick={() => setEditing(true)}>Editar</Button> : null}
            <Button variant="danger" onClick={() => setConfirming(true)}>Cancelar clase</Button>
          </div>
        )
      ) : null}
    </section>
  );
}
