"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useStudentEmails } from "@/modules/administration/api";
import { LessonAttendance } from "@/modules/booking/components/LessonAttendance";
import { useLesson } from "@/modules/lesson/api";
import { LessonDetail } from "@/modules/lesson/components/LessonDetail";
import { useTeacherZone } from "@/modules/teacher/api";
import { useNow } from "@/shared/useNow";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

export default function AdminLessonPage() {
  const { id } = useParams<{ id: string }>();
  const lesson = useLesson(id);
  const zone = useTeacherZone();
  const emailOf = useStudentEmails();
  const now = useNow();
  const started = lesson.data ? new Date(lesson.data.startsAt).getTime() <= now : false;

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <Link href="/admin/lessons" className="flex min-h-11 items-center gap-1 self-start font-semibold text-court"><Icon name="chevronLeft" />Volver a las clases</Link>
      <ErrorNotice error={lesson.error} />
      {lesson.data && zone ? (
        <>
          <LessonDetail lesson={lesson.data} zone={zone} started={started} as="admin" />
          <section className="flex flex-col gap-3">
            <h2 className="font-display text-2xl font-semibold">Alumnos</h2>
            <LessonAttendance lessonId={id} started={started} nameOf={emailOf} as="admin" />
          </section>
        </>
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </div>
  );
}
