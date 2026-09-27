"use client";

import Link from "next/link";
import { useState } from "react";
import { useMyBookings, type Booking } from "@/modules/booking/api";
import { formatDateTime, formatTime } from "@/shared/time";
import { useNow } from "@/shared/useNow";
import { Button, BUTTON_BASE, BUTTON_VARIANTS } from "@/shared/ui/Button";
import { Chip } from "@/shared/ui/Chip";
import { EmptyState } from "@/shared/ui/EmptyState";
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
  const now = useNow();

  if (bookings.error) {
    return <ErrorNotice error={bookings.error} />;
  }
  if (!bookings.data) {
    return <p className="text-muted">Cargando…</p>;
  }
  if (bookings.data.items.length === 0) {
    return (
      <EmptyState title="Aún no has reservado ninguna clase"
        actions={<Link href="/calendar" className={`${BUTTON_BASE} ${BUTTON_VARIANTS.primary}`}>Ver las clases</Link>}>
        Las clases con plazas están en Clases. Las que reserves aparecerán aquí.
      </EmptyState>
    );
  }

  const { items, size, totalItems } = bookings.data;
  // The API sends the latest lesson first; what is still to come reads better soonest first.
  const coming = items
    .filter((booking) => booking.status === "CONFIRMED" && Date.parse(booking.lesson.endsAt) > now)
    .toSorted((a, b) => a.lesson.startsAt.localeCompare(b.lesson.startsAt));
  const done = items.filter((booking) => !coming.includes(booking));

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      {coming.length > 0 ? <BookingList title="Próximas" bookings={coming} zone={zone} /> : null}
      {done.length > 0 ? <BookingList title="Anteriores y canceladas" bookings={done} zone={zone} /> : null}
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

function BookingList({ title, bookings, zone }: { title: string; bookings: Booking[]; zone: string }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="font-display text-2xl font-semibold">{title}</h2>
      <ul className="flex flex-col divide-y divide-line-soft rounded-xl border border-line bg-paper">
        {bookings.map((booking) => {
          const label = ATTENDANCE[booking.attendance] ?? STATUS[booking.status];
          return (
            <li key={booking.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
              <span className="font-semibold first-letter:uppercase">
                {formatDateTime(booking.lesson.startsAt, zone)}–{formatTime(booking.lesson.endsAt, zone)}
              </span>
              <Chip tone={toneOf(booking)}>{label}</Chip>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

function toneOf(booking: Booking): "success" | "court" | "neutral" | "warning" {
  if (booking.attendance === "ATTENDED") return "success";
  if (booking.attendance === "NO_SHOW") return "warning";
  return booking.status === "CONFIRMED" ? "court" : "neutral";
}
