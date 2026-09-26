"use client";

import { MyBookings } from "@/modules/booking/components/MyBookings";
import { useTeacherZone } from "@/modules/teacher/api";

export default function MyBookingsPage() {
  const zone = useTeacherZone();
  return (
    <>
      <h1 className="font-display text-3xl font-semibold">Mis reservas</h1>
      {zone ? <MyBookings zone={zone} /> : <p className="text-muted">Cargando…</p>}
    </>
  );
}
