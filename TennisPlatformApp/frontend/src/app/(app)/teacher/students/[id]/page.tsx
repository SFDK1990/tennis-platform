"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { StudentHistory } from "@/modules/booking/components/StudentHistory";
import { StudentGroups } from "@/modules/memberships/components/StudentGroups";
import { useTeacherPasses } from "@/modules/passes/api";
import { StudentPasses } from "@/modules/passes/components/StudentPasses";
import { useStudentDetail } from "@/modules/student/api";
import { StudentAvatar } from "@/modules/student/components/StudentAvatar";
import { StudentRecordForm } from "@/modules/student/components/StudentRecordForm";
import { useTeacherZone } from "@/modules/teacher/api";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

/** The student's record: who they are, their level and notes, their passes (Fase 27), their groups and fees (Fase 28) and their bookings (Fase 23). */
export default function StudentPage() {
  const { id } = useParams<{ id: string }>();
  const student = useStudentDetail(id);
  const zone = useTeacherZone();
  const passes = useTeacherPasses(id).data?.items ?? [];
  const passName = new Map(passes.map((pass) => [pass.id, pass.name]));

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <Link href="/teacher/students" className="flex min-h-11 items-center gap-1 self-start font-semibold text-court">
        <Icon name="chevronLeft" />Volver a alumnos
      </Link>
      <ErrorNotice error={student.error} />
      {student.data && zone ? (
        <>
          <div className="flex flex-wrap items-center gap-4">
            <StudentAvatar userId={student.data.userId} name={student.data.fullName} hasPhoto={student.data.hasPhoto}
              size="lg" />
            <div className="flex flex-col gap-1">
              <h1 className="font-display text-4xl font-semibold leading-none">{student.data.fullName}</h1>
              <p className="text-muted">
                {student.data.email}{student.data.phone ? `, ${student.data.phone}` : ""}
              </p>
            </div>
          </div>
          <section aria-labelledby="record-heading" className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
            <h2 id="record-heading" className="font-display text-2xl font-semibold leading-none">Ficha</h2>
            <StudentRecordForm key={student.data.userId} student={student.data} />
          </section>
          <section aria-labelledby="passes-heading" className="flex flex-col gap-3">
            <h2 id="passes-heading" className="font-display text-2xl font-semibold">Bonos</h2>
            <StudentPasses studentUserId={id} zone={zone} />
          </section>
          <section aria-labelledby="groups-heading" className="flex flex-col gap-3">
            <h2 id="groups-heading" className="font-display text-2xl font-semibold">Grupos fijos y mensualidades</h2>
            <StudentGroups studentUserId={id} zone={zone} />
          </section>
          <section aria-labelledby="history-heading" className="flex flex-col gap-3">
            <h2 id="history-heading" className="font-display text-2xl font-semibold">Historial</h2>
            <StudentHistory studentUserId={id} zone={zone} passNameOf={(passId) => passName.get(passId)} />
          </section>
        </>
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </div>
  );
}
