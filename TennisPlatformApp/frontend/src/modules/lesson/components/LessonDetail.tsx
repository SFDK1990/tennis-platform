"use client";

import { useState } from "react";
import { useCancelLesson, type Lesson } from "@/modules/lesson/api";
import { formatDateTime, formatTime } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

const STATUS: Record<Lesson["status"], string> = {
  OPEN: "Abierta",
  FULL: "Completa",
  CANCELLED: "Cancelada",
  COMPLETED: "Terminada",
};

export function LessonDetail({ lesson, zone }: { lesson: Lesson; zone: string }) {
  const cancel = useCancelLesson();
  const [confirming, setConfirming] = useState(false);
  const cancellable = lesson.status === "OPEN" || lesson.status === "FULL";

  return (
    <section className="flex flex-col gap-3">
      <h1 className="font-display text-3xl font-semibold first-letter:uppercase">
        {formatDateTime(lesson.startsAt, zone)}–{formatTime(lesson.endsAt, zone)}
      </h1>
      <p className="text-muted">
        {lesson.type === "INDIVIDUAL" ? "Clase individual" : `Clase de grupo, ${lesson.capacity} plazas`}.{" "}
        {STATUS[lesson.status]}, con {lesson.bookedCount} {lesson.bookedCount === 1 ? "reserva" : "reservas"}.
      </p>
      {lesson.notes ? <p className="rounded-md border border-line bg-paper px-4 py-3">{lesson.notes}</p> : null}
      {lesson.cancelledAtShortNotice ? (
        <Notice>Se canceló con menos de 24 horas de aviso.</Notice>
      ) : null}
      <ErrorNotice error={cancel.error} />
      {cancellable ? (
        confirming ? (
          <div className="flex flex-wrap items-center gap-2">
            <span>Se cancelarán también todas sus reservas.</span>
            <Button variant="danger" pending={cancel.isPending} onClick={() => cancel.mutate(lesson.id)}>
              Sí, cancelar la clase
            </Button>
            <Button variant="quiet" onClick={() => setConfirming(false)}>No</Button>
          </div>
        ) : (
          <Button variant="danger" className="self-start" onClick={() => setConfirming(true)}>Cancelar clase</Button>
        )
      ) : null}
    </section>
  );
}
