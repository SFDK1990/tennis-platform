"use client";

import Link from "next/link";
import { overdue, useFees } from "@/modules/memberships/api";

/** On "Hoy": how many months are past their due day without anything recorded. Nothing when none. */
export function OverdueNotice() {
  const fees = useFees();
  const late = overdue(fees.data?.items ?? []);
  if (late.length === 0) {
    return null;
  }
  const people = new Set(late.map((line) => line.studentUserId)).size;
  return (
    <p role="status" className="rounded-lg border border-fault/40 bg-fault-tint/60 px-4 py-3 text-fault">
      {late.length === 1 ? "Hay 1 mensualidad sin pagar" : `Hay ${late.length} mensualidades sin pagar`}
      {people > 1 ? ` de ${people} alumnos` : ""}.{" "}
      <Link href="/teacher/fees" className="font-semibold underline">Ver mensualidades</Link>
    </p>
  );
}
