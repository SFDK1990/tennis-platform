"use client";

import { MessageComposer, useGeneralAudiences } from "@/modules/messages/components/MessageComposer";
import { SentMessages } from "@/modules/messages/components/SentMessages";
import { useTeacherZone } from "@/modules/teacher/api";

/** The teacher writes to everybody or to a level; a lesson's students, from the lesson (Fase 24). */
export default function TeacherMessagesPage() {
  const zone = useTeacherZone();
  const audiences = useGeneralAudiences();
  return (
    <>
      <div className="flex max-w-3xl flex-col gap-1.5">
        <h1 className="font-display text-4xl font-semibold leading-none">Mensajes</h1>
        <p className="text-muted">Para escribir a los alumnos de una clase o de su grupo, hazlo desde la propia clase.</p>
      </div>
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:items-start">
        <MessageComposer options={audiences} />
        {zone ? <SentMessages zone={zone} /> : <p className="text-muted">Cargando…</p>}
      </div>
    </>
  );
}
