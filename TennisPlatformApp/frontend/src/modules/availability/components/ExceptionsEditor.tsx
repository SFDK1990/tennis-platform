"use client";

import { useState, type FormEvent } from "react";
import { useCreateException, useDeleteException, type AvailabilityException, type NewException } from "@/modules/availability/api";
import { formatDay, type LocalDate } from "@/shared/time";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { ErrorNotice } from "@/shared/ui/Notice";

const hhmm = (time: string | null) => time?.slice(0, 5) ?? "";

function describe(exception: AvailabilityException): string {
  const hours = exception.startTime ? `de ${hhmm(exception.startTime)} a ${hhmm(exception.endTime)}` : "todo el día";
  return exception.type === "BLOCK" ? `No disponible ${hours}` : `Disponible además ${hours}`;
}

/** Days off and extra hours. When both touch the same hours, the block wins. */
export function ExceptionsEditor({ exceptions, today }: { exceptions: AvailabilityException[]; today: LocalDate }) {
  const create = useCreateException();
  const remove = useDeleteException();
  const [type, setType] = useState<NewException["type"]>("BLOCK");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const startTime = String(form.get("startTime") ?? "") || null;
    const endTime = String(form.get("endTime") ?? "") || null;
    create.mutate({ date: String(form.get("date")), type, startTime, endTime });
  }

  const sorted = exceptions.toSorted((a, b) => a.date.localeCompare(b.date));

  return (
    <div className="flex flex-col gap-4">
      {sorted.length === 0 ? (
        <p className="text-muted">Ninguna en los próximos dos meses.</p>
      ) : (
        <ul className="flex flex-col divide-y divide-line rounded-md border border-line bg-paper">
          {sorted.map((exception) => (
            <li key={exception.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
              <span>
                <span className="font-semibold first-letter:uppercase">{formatDay(exception.date)}</span>:{" "}
                {describe(exception)}
              </span>
              <Button variant="quiet" pending={remove.isPending && remove.variables === exception.id}
                onClick={() => remove.mutate(exception.id)}>
                Quitar
              </Button>
            </li>
          ))}
        </ul>
      )}
      <form onSubmit={submit} className="flex flex-wrap items-end gap-3">
        <Field label="Día" name="date" type="date" min={today} defaultValue={today} required />
        <label className="flex flex-col gap-1 text-sm font-semibold">
          Tipo
          <select value={type} onChange={(e) => setType(e.target.value as NewException["type"])}
            className="min-h-10 rounded-md border border-line bg-paper px-3 font-normal">
            <option value="BLOCK">No disponible</option>
            <option value="EXTRA">Horas extra</option>
          </select>
        </label>
        <Field label="Desde" name="startTime" type="time" step={1800} required={type === "EXTRA"}
          hint={type === "BLOCK" ? "Vacío: todo el día" : undefined} />
        <Field label="Hasta" name="endTime" type="time" step={1800} required={type === "EXTRA"} />
        <Button type="submit" pending={create.isPending}>Añadir</Button>
      </form>
      <ErrorNotice error={create.error ?? remove.error} />
    </div>
  );
}
