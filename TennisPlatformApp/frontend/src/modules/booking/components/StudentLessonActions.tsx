"use client";

import { useState } from "react";
import {
  insideCancellationWindow, useBookLesson, useCancelBooking, useJoinWaitingList, useLeaveWaitingList, useMyWaitingList,
} from "@/modules/booking/api";
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
          <span className="rounded-lg bg-surround px-3 py-2 text-sm font-semibold text-white">Tienes plaza</span>
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

  if (lessonStatus === "FULL" && !started) {
    return late ? null : <WaitingListAction lessonId={lessonId} />;
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

/**
 * A full lesson more than 24 hours away: the student waits for a seat, which is booked for them
 * the moment one frees up (34-fase21-analisis-lista-espera.md). Closer than that the list is
 * closed, and nothing is offered.
 */
function WaitingListAction({ lessonId }: { lessonId: string }) {
  const places = useMyWaitingList();
  const join = useJoinWaitingList();
  const leave = useLeaveWaitingList();
  const place = places.data?.find((candidate) => candidate.lesson.id === lessonId);

  return (
    <div className="flex flex-col items-end gap-2">
      {place ? (
        <div className="flex flex-wrap items-center justify-end gap-2">
          <span className="text-sm font-semibold">{waitingLabel(place.position)}</span>
          <Button variant="quiet" pending={leave.isPending} onClick={() => leave.mutate(lessonId)}>Salir de la lista</Button>
        </div>
      ) : (
        <div className="flex flex-wrap items-center justify-end gap-2">
          <span className="max-w-48 text-xs text-muted">Si se libera una plaza, es para el primero de la lista.</span>
          <Button variant="secondary" pending={join.isPending} onClick={() => join.mutate(lessonId)}>
            Apuntarme a la lista de espera
          </Button>
        </div>
      )}
      <ErrorNotice error={join.error ?? leave.error} />
    </div>
  );
}

/** Where the student stands, said without gender: the next seat, or how many are ahead. */
export function waitingLabel(position: number): string {
  return position === 1 ? "En lista de espera: la próxima plaza es tuya" : `En lista de espera: ${position - 1} por delante`;
}
