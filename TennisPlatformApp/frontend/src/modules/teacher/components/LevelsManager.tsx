"use client";

import { useState, type FormEvent } from "react";
import { useCreateLevel, useDeleteLevel, useUpdateLevel } from "@/modules/teacher/api";
import { ApiError } from "@/shared/api/errors";
import { useLevels, type Level } from "@/shared/levels";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Field";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * The teacher's levels, in order (36-fase23, decision 1). A level a student or a lesson still has
 * cannot be deleted: the message says so instead of a generic error.
 */
export function LevelsManager() {
  const levels = useLevels();
  const create = useCreateLevel();
  const remove = useDeleteLevel();
  const inUse = remove.error instanceof ApiError && remove.error.code === "LEVEL_IN_USE";

  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const name = String(new FormData(form).get("name")).trim();
    if (name) create.mutate(name, { onSuccess: () => form.reset() });
  }

  return (
    <section aria-labelledby="levels-heading" className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
      <div className="flex flex-col gap-1">
        <h2 id="levels-heading" className="font-display text-2xl font-semibold leading-none">Niveles</h2>
        <p className="text-sm text-muted">
          Un alumno reserva las clases de su nivel y las abiertas a todos. El orden es de menos a más.
        </p>
      </div>
      <ErrorNotice error={levels.error} />
      <ol className="flex flex-col divide-y divide-line-soft rounded-lg border border-line">
        {levels.data?.map((level, index) => (
          <LevelRow key={level.id} level={level} first={index === 0} last={index === levels.data.length - 1}
            onDelete={() => remove.mutate(level.id)} deleting={remove.isPending && remove.variables === level.id} />
        ))}
      </ol>
      {inUse ? (
        <p role="alert" className="text-sm text-fault">
          Ese nivel lo tiene algún alumno o alguna clase. Cámbialos de nivel antes de borrarlo.
        </p>
      ) : <ErrorNotice error={remove.error} />}
      <form onSubmit={add} className="flex flex-wrap items-end gap-3">
        <Field label="Nuevo nivel" name="name" maxLength={60} className="min-w-48 flex-1" />
        <Button type="submit" variant="quiet" pending={create.isPending}>Añadir</Button>
      </form>
      <ErrorNotice error={create.error} />
    </section>
  );
}

interface LevelRowProps {
  level: Level;
  first: boolean;
  last: boolean;
  deleting: boolean;
  onDelete: () => void;
}

function LevelRow({ level, first, last, deleting, onDelete }: LevelRowProps) {
  const update = useUpdateLevel();
  const [name, setName] = useState(level.name);
  const renamed = name.trim() !== "" && name.trim() !== level.name;

  return (
    <li className="flex flex-wrap items-center gap-2 px-3 py-2">
      <label className="sr-only" htmlFor={`level-${level.id}`}>Nombre del nivel {level.position}</label>
      <input id={`level-${level.id}`} value={name} maxLength={60} onChange={(event) => setName(event.target.value)}
        className="min-h-11 min-w-40 flex-1 rounded-lg border border-transparent bg-transparent px-2 font-semibold hover:border-line focus:border-court" />
      {renamed ? (
        <Button variant="quiet" pending={update.isPending} onClick={() => update.mutate({ id: level.id, name: name.trim() })}>
          Guardar
        </Button>
      ) : null}
      <Button variant="quiet" aria-label={`Subir ${level.name}`} disabled={first}
        onClick={() => update.mutate({ id: level.id, position: level.position - 1 })}>
        <Icon name="chevronLeft" className="size-5 rotate-90" />
      </Button>
      <Button variant="quiet" aria-label={`Bajar ${level.name}`} disabled={last}
        onClick={() => update.mutate({ id: level.id, position: level.position + 1 })}>
        <Icon name="chevronRight" className="size-5 rotate-90" />
      </Button>
      <Button variant="danger" pending={deleting} onClick={onDelete}>Borrar</Button>
      {update.error ? <span className="w-full"><ErrorNotice error={update.error} /></span> : null}
    </li>
  );
}
