"use client";

import { MonthSummaryReport } from "@/modules/calendar/components/MonthSummaryReport";

export default function TeacherSummaryPage() {
  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Resumen del mes</h1>
      <MonthSummaryReport />
    </>
  );
}
