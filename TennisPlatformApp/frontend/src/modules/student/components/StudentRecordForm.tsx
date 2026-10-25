"use client";

import { useState, type FormEvent } from "react";
import { useUpdateStudentRecord, type StudentDetail } from "@/modules/student/api";
import { LevelSelect } from "@/shared/levels";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

/**
 * What the teacher keeps about a student (36-fase23, decision 6): their level, what to work on,
 * and notes the student does not see in the app. All three are saved together.
 */
export function StudentRecordForm({ student }: { student: StudentDetail }) {
  const save = useUpdateStudentRecord(student.userId);
  const [levelId, setLevelId] = useState<string | null>(student.levelId);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    save.mutate({
      levelId,
      goals: String(form.get("goals") ?? "") || null,
      privateNotes: String(form.get("privateNotes") ?? "") || null,
    });
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="max-w-64">
        <LevelSelect id="student-level" label="Nivel" noneLabel="Sin nivel (reserva cualquier clase)" value={levelId}
          onChange={setLevelId} />
      </div>
      <TextArea id="goals" label="Objetivos" hint="Lo que quieres trabajar con este alumno." defaultValue={student.goals} />
      <TextArea id="privateNotes" label="Notas privadas"
        hint="El alumno no las ve en la app. Sí salen si pide la descarga de sus datos."
        defaultValue={student.privateNotes} />
      <ErrorNotice error={save.error} />
      {save.isSuccess ? <Notice tone="success">Ficha guardada.</Notice> : null}
      <Button type="submit" pending={save.isPending} className="self-start">Guardar ficha</Button>
    </form>
  );
}

function TextArea({ id, label, hint, defaultValue }: { id: string; label: string; hint: string; defaultValue: string | null }) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold">{label}</label>
      <textarea id={id} name={id} rows={3} maxLength={2000} defaultValue={defaultValue ?? ""}
        aria-describedby={`${id}-hint`} className={`${CONTROL_CLASS} min-h-24 py-2`} />
      <span id={`${id}-hint`} className="text-xs text-muted">{hint}</span>
    </div>
  );
}
