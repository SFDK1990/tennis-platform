"use client";

import { useState } from "react";
import { useCancelBooking, useLessonBookings, useMarkAttendance, type AttendanceMark } from "@/modules/booking/api";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

interface LessonAttendanceProps {
  lessonId: string;
  started: boolean;
  /** Bookings only carry the student's id; the page knows the names. */
  nameOf: (studentUserId: string) => string;
}

/**
 * The teacher's view of who is coming. Before the lesson they can take a seat back; once it
 * has started, they mark who came, all at once, as the backend applies it.
 */
export function LessonAttendance({ lessonId, started, nameOf }: LessonAttendanceProps) {
  const bookings = useLessonBookings(lessonId);
  const mark = useMarkAttendance(lessonId);
  const cancel = useCancelBooking();
  const [marks, setMarks] = useState<Record<string, AttendanceMark["status"]>>({});

  if (bookings.error) {
    return <ErrorNotice error={bookings.error} />;
  }
  if (!bookings.data) {
    return <p className="text-muted">Cargando…</p>;
  }

  const confirmed = bookings.data.items.filter((booking) => booking.status === "CONFIRMED");
  if (confirmed.length === 0) {
    return <p className="text-muted">Nadie ha reservado esta clase todavía.</p>;
  }

  const markOf = (bookingId: string, current: string) =>
    marks[bookingId] ?? (current === "PENDING" ? undefined : (current as AttendanceMark["status"]));
  const entries = confirmed.flatMap((booking) => {
    const status = markOf(booking.id, booking.attendance);
    return status ? [{ bookingId: booking.id, status }] : [];
  });

  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col divide-y divide-line rounded-md border border-line bg-paper">
        {confirmed.map((booking) => (
          <li key={booking.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
            <span className="font-semibold">{nameOf(booking.studentUserId)}</span>
            {started ? (
              <fieldset className="flex gap-4">
                <legend className="sr-only">Asistencia de {nameOf(booking.studentUserId)}</legend>
                {(["ATTENDED", "NO_SHOW"] as const).map((status) => (
                  <label key={status} className="flex items-center gap-2">
                    <input
                      type="radio"
                      name={booking.id}
                      checked={markOf(booking.id, booking.attendance) === status}
                      onChange={() => setMarks((current) => ({ ...current, [booking.id]: status }))}
                    />
                    {status === "ATTENDED" ? "Vino" : "No vino"}
                  </label>
                ))}
              </fieldset>
            ) : (
              <Button variant="danger" pending={cancel.isPending && cancel.variables === booking.id}
                onClick={() => cancel.mutate(booking.id)}>
                Quitar plaza
              </Button>
            )}
          </li>
        ))}
      </ul>
      <ErrorNotice error={mark.error ?? cancel.error} />
      {mark.isSuccess ? <Notice tone="success">Asistencia guardada.</Notice> : null}
      {started ? (
        <Button className="self-start" disabled={entries.length === 0} pending={mark.isPending}
          onClick={() => mark.mutate(entries)}>
          Guardar asistencia
        </Button>
      ) : (
        <p className="text-sm text-muted">La asistencia se marca cuando empiece la clase.</p>
      )}
    </div>
  );
}
