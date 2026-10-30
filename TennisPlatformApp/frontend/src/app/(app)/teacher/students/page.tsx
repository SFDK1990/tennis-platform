"use client";

import Link from "next/link";
import { useTeacherPasses } from "@/modules/passes/api";
import { PassFlag } from "@/modules/passes/components/PassFlag";
import { StudentsManager } from "@/modules/student/components/StudentsManager";
import { useTeacherZone } from "@/modules/teacher/api";

/** The students, each with a warning when they have run out of passes or one is about to expire (Fase 27). */
export default function StudentsPage() {
  const passes = useTeacherPasses().data?.items ?? [];
  const zone = useTeacherZone() ?? "Europe/Madrid";
  return (
    <>
      <div className="flex flex-wrap items-baseline justify-between gap-3">
        <h1 className="font-display text-4xl font-semibold leading-none">Alumnos</h1>
        <Link href="/teacher/passes" className="font-semibold text-court">Tipos de bono</Link>
      </div>
      <StudentsManager flagFor={(studentUserId) => (
        <PassFlag passes={passes.filter((pass) => pass.studentUserId === studentUserId)} zone={zone} />
      )} />
    </>
  );
}
