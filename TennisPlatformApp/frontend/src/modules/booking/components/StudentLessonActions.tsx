"use client";

import { useState } from "react";
import { insideCancellationWindow, useBookLesson, useCancelBooking } from "@/modules/booking/api";
import { useNow } from "@/shared/useNow";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/Notice";

interface StudentLessonActionsProps {
  lessonId: string;
  startsAt: string;
  lessonStatus: string;
  booking: { id: string; status: string } | null;
}

/** Book or cancel one lesson, with the 24-hour rule said before it bites. */
export function StudentLessonActions({ lessonId, startsAt, lessonStatus, booking }: StudentLessonActionsProps) {
  const book = useBookLesson();
  const cancel = useCancelBooking();
  const [confirming, setConfirming] = useState(false);
  const now = useNow();
  const late = insideCancellationWindow(startsAt, now);
  const started = new Date(startsAt).getTime() <= now;
  const error = book.error ?? cancel.error;

  if (booking?.status === "CONFIRMED") {
    return (
      <div className="flex flex-col items-end gap-2">
        <div className="flex items-center gap-2">
          <span className="rounded-md bg-surround px-3 py-2 text-sm font-semibold text-white">Tienes plaza</span>
          {started ? null : late ? (
            <span className="max-w-40 text-xs text-muted">Faltan menos de 24 h: ya no se puede cancelar</span>
          ) : confirming ? (
            <>
              <Button variant="danger" pending={cancel.isPending} onClick={() => cancel.mutate(booking.id)}>
                Sí, cancelar
              </Button>
              <Button variant="quiet" onClick={() => setConfirming(false)}>No</Button>
            </>
          ) : (
            <Button variant="quiet" onClick={() => setConfirming(true)}>Cancelar reserva</Button>
          )}
        </div>
        <ErrorNotice error={error} />
      </div>
    );
  }

  if (lessonStatus !== "OPEN" || started) {
    return null;
  }

  return (
    <div className="flex flex-col items-end gap-2">
      {late && confirming ? (
        <div className="flex flex-wrap items-center justify-end gap-2">
          <span className="max-w-48 text-xs">Empieza en menos de 24 h: si reservas, no podrás cancelar.</span>
          <Button variant="primary" pending={book.isPending} onClick={() => book.mutate(lessonId)}>Reservar igualmente</Button>
          <Button variant="quiet" onClick={() => setConfirming(false)}>No</Button>
        </div>
      ) : (
        <Button variant="primary" pending={book.isPending} onClick={() => (late ? setConfirming(true) : book.mutate(lessonId))}>
          Reservar
        </Button>
      )}
      <ErrorNotice error={error} />
    </div>
  );
}
