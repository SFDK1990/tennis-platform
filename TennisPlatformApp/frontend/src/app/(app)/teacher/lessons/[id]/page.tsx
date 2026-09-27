"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { LessonAttendance } from "@/modules/booking/components/LessonAttendance";
import { useLesson } from "@/modules/lesson/api";
import { LessonDetail } from "@/modules/lesson/components/LessonDetail";
import { useStudentNames } from "@/modules/student/api";
import { useTeacherZone } from "@/modules/teacher/api";
import { useNow } from "@/shared/useNow";
import { ErrorNotice } from "@/shared/ui/Notice";

export default function LessonPage() {
  const { id } = useParams<{ id: string }>();
  const lesson = useLesson(id);
  const zone = useTeacherZone();
  const nameOf = useStudentNames();
  const now = useNow();

  return (
    <>
      <Link href="/teacher" className="text-sm text-court underline">Volver al calendario</Link>
      <ErrorNotice error={lesson.error} />
      {lesson.data && zone ? (
        <>
          <LessonDetail lesson={lesson.data} zone={zone} />
          <section className="flex flex-col gap-3">
            <h2 className="font-display text-2xl font-semibold">Alumnos</h2>
            <LessonAttendance lessonId={id} started={new Date(lesson.data.startsAt).getTime() <= now} nameOf={nameOf} />
          </section>
        </>
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </>
  );
}
