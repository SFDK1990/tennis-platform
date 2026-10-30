"use client";

import { useStudentHistory, type Booking } from "@/modules/booking/api";
import { formatDateTime } from "@/shared/time";
import { Chip } from "@/shared/ui/Chip";
import { ErrorNotice } from "@/shared/ui/Notice";

const OUTCOME: Record<Booking["status"], string> = {
  CONFIRMED: "Reservada",
  CANCELLED_BY_STUDENT: "La canceló",
  CANCELLED_BY_TEACHER: "La cancelaste",
  CANCELLED_BY_ADMIN: "Cancelada por administración",
};

const ATTENDANCE: Record<Booking["attendance"], string | null> = {
  PENDING: null,
  ATTENDED: "Vino",
  NO_SHOW: "No vino",
};

/**
 * The student's bookings with the teacher, newest first, with what happened in each (Fase 23) and
 * the pass it was paid with, when there was one (Fase 27).
 */
export function StudentHistory({ studentUserId, zone, passNameOf }: {
  studentUserId: string;
  zone: string;
  passNameOf?: (passId: string) => string | undefined;
}) {
  const history = useStudentHistory(studentUserId);

  if (history.error) {
    return <ErrorNotice error={history.error} />;
  }
  if (!history.data) {
    return <p className="text-muted">Cargando…</p>;
  }
  if (history.data.items.length === 0) {
    return <p className="text-muted">Todavía no ha reservado ninguna clase.</p>;
  }
  return (
    <ul className="flex flex-col divide-y divide-line-soft rounded-xl border border-line bg-paper">
      {history.data.items.map((booking) => {
        const attendance = ATTENDANCE[booking.attendance];
        return (
          <li key={booking.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
            <span className="flex flex-col">
              <span className="font-semibold first-letter:uppercase">{formatDateTime(booking.lesson.startsAt, zone)}</span>
              {booking.passId && passNameOf?.(booking.passId) ? (
                <span className="text-sm text-muted">Con el bono {passNameOf(booking.passId)}</span>
              ) : null}
            </span>
            <Chip tone={attendance === "No vino" ? "warning" : attendance ? "success" : booking.status === "CONFIRMED" ? "court" : "neutral"}>
              {attendance ?? OUTCOME[booking.status]}
            </Chip>
          </li>
        );
      })}
    </ul>
  );
}
