"use client";

import { FeesBoard } from "@/modules/memberships/components/FeesBoard";
import { GroupsManager } from "@/modules/memberships/components/GroupsManager";
import { useManagedStudents, useStudentNames } from "@/modules/student/api";
import { useTeacherZone } from "@/modules/teacher/api";

/** The fixed groups and their monthly fees (Fase 28). */
export default function FeesPage() {
  const zone = useTeacherZone();
  const nameOf = useStudentNames();
  const students = (useManagedStudents().data?.items ?? [])
    .filter((student) => student.managedStatus === "MANAGED")
    .map((student) => ({ userId: student.userId, name: student.fullName }));

  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Mensualidades</h1>
      {zone ? (
        <div className="flex max-w-3xl flex-col gap-6">
          <FeesBoard zone={zone} nameOf={nameOf} />
          <GroupsManager zone={zone} nameOf={nameOf} students={students} />
        </div>
      ) : <p className="text-muted">Cargando…</p>}
    </>
  );
}
