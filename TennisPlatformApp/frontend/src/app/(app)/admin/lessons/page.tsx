"use client";

import { AgendaWeek } from "@/modules/calendar/components/AgendaWeek";
import { useTeacherZone } from "@/modules/teacher/api";

const hrefFor = (lesson: { id: string }) => `/admin/lessons/${lesson.id}`;

/** The teacher's diary, to resolve an incident: the administrator sees it and cancels from the lesson. */
export default function AdminLessonsPage() {
  const zone = useTeacherZone() ?? "Europe/Madrid";
  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Clases</h1>
      <AgendaWeek zone={zone} hrefFor={hrefFor} />
    </>
  );
}
