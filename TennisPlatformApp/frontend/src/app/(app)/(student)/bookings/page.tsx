"use client";

import { MyBookings } from "@/modules/booking/components/MyBookings";
import { MyPasses } from "@/modules/passes/components/MyPasses";
import { useTeacherZone } from "@/modules/teacher/api";

export default function MyBookingsPage() {
  const zone = useTeacherZone();
  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Mis reservas</h1>
      <div className="max-w-3xl"><MyPasses /></div>
      {zone ? <MyBookings zone={zone} /> : <p className="text-muted">Cargando…</p>}
    </>
  );
}
