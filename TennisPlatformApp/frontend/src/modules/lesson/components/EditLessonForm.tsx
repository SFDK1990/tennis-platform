"use client";

import type { FormEvent } from "react";
import { useEditLesson, type Lesson } from "@/modules/lesson/api";
import { fieldErrorOf } from "@/shared/api/errors";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * The notes and, in a group lesson, the seats. The time is not here on purpose: the students
 * booked that time, and moving it is cancelling and creating another lesson.
 */
export function EditLessonForm({ lesson, onDone }: { lesson: Lesson; onDone: () => void }) {
  const edit = useEditLesson(lesson.id);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    edit.mutate(
      {
        notes: String(form.get("notes") ?? ""),
        capacity: lesson.type === "GROUP" ? Number(form.get("capacity")) : undefined,
      },
      { onSuccess: onDone },
    );
  }

  return (
    <form onSubmit={submit} aria-label="Editar la clase" className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
      <Field label="Notas" name="notes" defaultValue={lesson.notes ?? ""} hint="Sólo las ves tú. Déjalas en blanco para quitarlas." />
      {lesson.type === "GROUP" ? (
        <Field label="Plazas" name="capacity" type="number" min={Math.max(lesson.bookedCount, 1)} required
          defaultValue={lesson.capacity} error={fieldErrorOf(edit.error, "capacity")}
          hint={lesson.bookedCount > 0 ? `No puede bajar de ${lesson.bookedCount}: ya hay alumnos apuntados.` : undefined} />
      ) : null}
      <ErrorNotice error={edit.error} />
      <div className="flex flex-wrap gap-2">
        <Button type="submit" pending={edit.isPending}>Guardar cambios</Button>
        <Button type="button" variant="quiet" onClick={onDone}>Cancelar</Button>
      </div>
    </form>
  );
}
