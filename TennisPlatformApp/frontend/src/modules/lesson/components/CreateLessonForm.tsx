"use client";

import { useState, type FormEvent } from "react";
import { useCreateLesson, type CreateLesson } from "@/modules/lesson/api";
import { ApiError } from "@/shared/api/errors";
import { zonedToInstant, type LocalDate } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

const DURATIONS = [30, 60, 90, 120];

/** Times are the teacher's wall clock, turned into instants in their zone before sending. */
export function CreateLessonForm({ zone, defaultDate }: { zone: string; defaultDate: LocalDate }) {
  const create = useCreateLesson();
  const [type, setType] = useState<CreateLesson["type"]>("INDIVIDUAL");
  const outsideHours = create.error instanceof ApiError && create.error.code === "LESSON_OUTSIDE_AVAILABILITY";

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const startsAt = zonedToInstant(String(form.get("date")), String(form.get("time")), zone);
    const minutes = Number(form.get("duration"));
    create.mutate({
      type,
      startsAt,
      endsAt: new Date(new Date(startsAt).getTime() + minutes * 60_000).toISOString(),
      capacity: type === "INDIVIDUAL" ? 1 : Number(form.get("capacity")),
      notes: String(form.get("notes") ?? "") || null,
      overrideAvailability: form.get("override") === "on",
    });
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Field label="Día" name="date" type="date" defaultValue={defaultDate} required />
        <Field label="Hora" name="time" type="time" step={1800} defaultValue="18:00" required />
        <div className="flex flex-col gap-1">
          <label htmlFor="duration" className="text-sm font-semibold">Duración</label>
          <select id="duration" name="duration" defaultValue={60} className="min-h-10 rounded-md border border-line bg-paper px-3">
            {DURATIONS.map((minutes) => <option key={minutes} value={minutes}>{minutes} min</option>)}
          </select>
        </div>
        <fieldset className="flex flex-col gap-1">
          <legend className="text-sm font-semibold">Tipo</legend>
          <div className="flex min-h-10 items-center gap-4">
            {(["INDIVIDUAL", "GROUP"] as const).map((option) => (
              <label key={option} className="flex items-center gap-2">
                <input type="radio" name="type" checked={type === option} onChange={() => setType(option)} />
                {option === "INDIVIDUAL" ? "Individual" : "Grupo"}
              </label>
            ))}
          </div>
        </fieldset>
      </div>
      {type === "GROUP" ? (
        <Field label="Plazas" name="capacity" type="number" min={1} max={8} defaultValue={4} required className="max-w-32" />
      ) : null}
      <Field label="Notas" name="notes" hint="Sólo las ves tú." />
      {outsideHours ? (
        <label className="flex items-center gap-2">
          <input type="checkbox" name="override" /> Crear fuera de horario
        </label>
      ) : null}
      <ErrorNotice error={create.error} />
      {create.isSuccess ? <Notice tone="success">Clase creada. Ya la ven tus alumnos.</Notice> : null}
      <Button type="submit" pending={create.isPending} className="self-start">Crear clase</Button>
    </form>
  );
}
