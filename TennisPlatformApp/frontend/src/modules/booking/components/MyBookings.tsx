"use client";

import { useState } from "react";
import { useMyBookings, type Booking } from "@/modules/booking/api";
import { formatDateTime, formatTime } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/Notice";

const STATUS: Record<Booking["status"], string> = {
  CONFIRMED: "Confirmada",
  CANCELLED_BY_STUDENT: "La cancelaste",
  CANCELLED_BY_TEACHER: "Cancelada por el profesor",
  CANCELLED_BY_ADMIN: "Cancelada por administración",
};

const ATTENDANCE: Record<Booking["attendance"], string | null> = {
  PENDING: null,
  ATTENDED: "Asististe",
  NO_SHOW: "No asististe",
};

export function MyBookings({ zone }: { zone: string }) {
  const [page, setPage] = useState(0);
  const bookings = useMyBookings(page);

  if (bookings.error) {
    return <ErrorNotice error={bookings.error} />;
  }
  if (!bookings.data) {
    return <p className="text-muted">Cargando…</p>;
  }
  if (bookings.data.items.length === 0) {
    return <p className="text-muted">Aún no has reservado ninguna clase. Las tienes en el calendario.</p>;
  }

  const { items, size, totalItems } = bookings.data;
  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col divide-y divide-line rounded-md border border-line bg-paper">
        {items.map((booking) => (
          <li key={booking.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
            <span className="font-semibold first-letter:uppercase">
              {formatDateTime(booking.lesson.startsAt, zone)}–{formatTime(booking.lesson.endsAt, zone)}
            </span>
            <span className={booking.status === "CONFIRMED" ? "text-surround" : "text-muted"}>
              {ATTENDANCE[booking.attendance] ?? STATUS[booking.status]}
            </span>
          </li>
        ))}
      </ul>
      {totalItems > size ? (
        <div className="flex justify-between">
          <Button variant="quiet" disabled={page === 0} onClick={() => setPage(page - 1)}>Más recientes</Button>
          <Button variant="quiet" disabled={(page + 1) * size >= totalItems} onClick={() => setPage(page + 1)}>
            Anteriores
          </Button>
        </div>
      ) : null}
    </div>
  );
}
