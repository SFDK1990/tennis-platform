"use client";

import { useState, type FormEvent } from "react";
import { useAddObjective, useRetireObjective, useUpdateObjective } from "@/modules/teacher/api";
import type { Level } from "@/shared/levels";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

type Objective = Level["objectives"][number];

/**
 * The objectives of a level, in order (44-fase29-analisis-progreso.md). Removing one retires it:
 * a student who achieved it keeps it.
 */
export function LevelObjectives({ level }: { level: Level }) {
  const add = useAddObjective();
  const retire = useRetireObjective();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const title = String(new FormData(form).get("title")).trim();
    if (title) add.mutate({ levelId: level.id, title }, { onSuccess: () => form.reset() });
  }

  return (
    <div className="flex w-full flex-col gap-2 pl-2">
      {level.objectives.length === 0 ? (
        <p className="text-sm text-muted">
          Sin objetivos. Escribe lo que un alumno de {level.name} tiene que conseguir, y lo marcarás en su ficha.
        </p>
      ) : (
        <ol aria-label={`Objetivos de ${level.name}`} className="flex flex-col gap-1">
          {level.objectives.map((objective, index) => (
            <ObjectiveRow key={objective.id} objective={objective} first={index === 0}
              last={index === level.objectives.length - 1} onRetire={() => retire.mutate(objective.id)}
              retiring={retire.isPending && retire.variables === objective.id} />
          ))}
        </ol>
      )}
      <ErrorNotice error={retire.error} />
      <form onSubmit={submit} className="flex flex-wrap items-center gap-2">
        <label className="sr-only" htmlFor={`objective-new-${level.id}`}>Nuevo objetivo de {level.name}</label>
        <input id={`objective-new-${level.id}`} name="title" maxLength={120} placeholder="Nuevo objetivo"
          className="min-h-11 min-w-48 flex-1 rounded-lg border border-line bg-paper px-3 text-sm focus:border-court" />
        <Button type="submit" variant="quiet" pending={add.isPending}>Añadir objetivo</Button>
      </form>
      <ErrorNotice error={add.error} />
    </div>
  );
}

interface ObjectiveRowProps {
  objective: Objective;
  first: boolean;
  last: boolean;
  retiring: boolean;
  onRetire: () => void;
}

function ObjectiveRow({ objective, first, last, retiring, onRetire }: ObjectiveRowProps) {
  const update = useUpdateObjective();
  const [title, setTitle] = useState(objective.title);
  const renamed = title.trim() !== "" && title.trim() !== objective.title;

  return (
    <li className="flex flex-wrap items-center gap-1">
      <label className="sr-only" htmlFor={`objective-${objective.id}`}>Objetivo {objective.position}</label>
      <input id={`objective-${objective.id}`} value={title} maxLength={120} onChange={(event) => setTitle(event.target.value)}
        className="min-h-11 min-w-40 flex-1 rounded-lg border border-transparent bg-transparent px-2 text-sm hover:border-line focus:border-court" />
      {renamed ? (
        <Button variant="quiet" pending={update.isPending}
          onClick={() => update.mutate({ id: objective.id, title: title.trim() })}>
          Guardar
        </Button>
      ) : null}
      <Button variant="quiet" aria-label={`Subir ${objective.title}`} disabled={first}
        onClick={() => update.mutate({ id: objective.id, position: objective.position - 1 })}>
        <Icon name="chevronLeft" className="size-5 rotate-90" />
      </Button>
      <Button variant="quiet" aria-label={`Bajar ${objective.title}`} disabled={last}
        onClick={() => update.mutate({ id: objective.id, position: objective.position + 1 })}>
        <Icon name="chevronRight" className="size-5 rotate-90" />
      </Button>
      <Button variant="quiet" aria-label={`Quitar ${objective.title}`} pending={retiring} onClick={onRetire}>
        Quitar
      </Button>
      {update.error ? <span className="w-full"><ErrorNotice error={update.error} /></span> : null}
    </li>
  );
}
