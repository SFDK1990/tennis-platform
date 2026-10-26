"use client";

import { MessageBoard } from "@/modules/messages/components/MessageBoard";
import { useTeacherProfile } from "@/modules/teacher/api";

export default function MyMessagesPage() {
  const teacher = useTeacherProfile().data;
  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Avisos</h1>
      {teacher ? <MessageBoard zone={teacher.timezone} teacher={teacher.displayName} /> : <p className="text-muted">Cargando…</p>}
    </>
  );
}
