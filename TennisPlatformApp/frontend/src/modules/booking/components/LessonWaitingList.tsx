"use client";

import { useLessonWaitingList } from "@/modules/booking/api";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * Who waits for a seat, in the order they will get one (Fase 21). Only to read: the list hands
 * seats out by itself, and nobody is put ahead or taken off from here.
 */
export function LessonWaitingList({ lessonId, nameOf }: { lessonId: string; nameOf: (studentUserId: string) => string }) {
  const waiting = useLessonWaitingList(lessonId);

  if (waiting.error) {
    return <ErrorNotice error={waiting.error} />;
  }
  if (!waiting.data || waiting.data.length === 0) {
    return null;
  }
  return (
    <section className="flex flex-col gap-3">
      <h2 className="font-display text-2xl font-semibold">Lista de espera</h2>
      <p className="text-sm text-muted">Si se libera una plaza, se reserva sola para el primero y le llega un correo.</p>
      <ol className="flex flex-col divide-y divide-line-soft rounded-xl border border-line bg-paper">
        {waiting.data.map((entry) => (
          <li key={entry.studentUserId} className="flex items-center gap-3 px-4 py-3">
            <span className="w-6 text-right font-display text-lg font-semibold text-muted">{entry.position}</span>
            <span className="font-semibold">{nameOf(entry.studentUserId)}</span>
          </li>
        ))}
      </ol>
    </section>
  );
}
