"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { LessonAttendance } from "@/modules/booking/components/LessonAttendance";
import { LessonWaitingList } from "@/modules/booking/components/LessonWaitingList";
import { useLesson } from "@/modules/lesson/api";
import { AttachToGroup } from "@/modules/memberships/components/AttachToGroup";
import { LessonDetail } from "@/modules/lesson/components/LessonDetail";
import { MessageComposer, type AudienceOption } from "@/modules/messages/components/MessageComposer";
import { useStudentHasPhoto, useStudentNames } from "@/modules/student/api";
import { StudentAvatar } from "@/modules/student/components/StudentAvatar";
import { useTeacherZone } from "@/modules/teacher/api";
import { useNow } from "@/shared/useNow";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

/** The students of this lesson, or those of every lesson still to come in its weekly series (Fase 24). */
function audiencesOf(lessonId: string, inSeries: boolean): AudienceOption[] {
  const lesson: AudienceOption = { key: "LESSON", label: "Los de esta clase", audience: { audience: "LESSON", lessonId } };
  return inSeries
    ? [lesson, { key: "SERIES", label: "Todo el grupo de la serie", audience: { audience: "SERIES", lessonId } }]
    : [lesson];
}

export default function LessonPage() {
  const { id } = useParams<{ id: string }>();
  const lesson = useLesson(id);
  const zone = useTeacherZone();
  const nameOf = useStudentNames();
  const hasPhoto = useStudentHasPhoto();
  const faceOf = (userId: string) => <StudentAvatar userId={userId} name={nameOf(userId)} hasPhoto={hasPhoto(userId)} />;
  const now = useNow();
  const started = lesson.data ? new Date(lesson.data.startsAt).getTime() <= now : false;

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <Link href="/teacher/calendar" className="flex min-h-11 items-center gap-1 self-start font-semibold text-court"><Icon name="chevronLeft" />Volver a la agenda</Link>
      <ErrorNotice error={lesson.error} />
      {lesson.data && zone ? (
        <>
          <LessonDetail lesson={lesson.data} zone={zone} started={started} />
          <section className="flex flex-col gap-3">
            <h2 className="font-display text-2xl font-semibold">Alumnos</h2>
            <LessonAttendance lessonId={id} started={started} nameOf={nameOf} faceOf={faceOf} />
          </section>
          {started ? null : <LessonWaitingList lessonId={id} nameOf={nameOf} />}
          {lesson.data.seriesId && lesson.data.type === "GROUP" && lesson.data.status !== "CANCELLED" ? (
            <AttachToGroup seriesId={lesson.data.seriesId} />
          ) : null}
          {lesson.data.status === "CANCELLED" ? null : (
            <MessageComposer title="Escribir a los alumnos" options={audiencesOf(id, lesson.data.seriesId != null)} />
          )}
        </>
      ) : (
        <p className="text-muted">Cargando…</p>
      )}
    </div>
  );
}
