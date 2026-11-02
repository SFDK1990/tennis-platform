"use client";

import { useMarkObjective, useStudentProgress, useUnmarkObjective, type LevelProgress } from "@/modules/student/api";
import { formatShortDay } from "@/shared/time";
import { Chip } from "@/shared/ui/Chip";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * The objectives of every level in a student's record, which the teacher marks as achieved
 * (44-fase29-analisis-progreso.md). Any level, not only theirs: what they already do of the next
 * one is there when they move up. Marking one tells the student on their board.
 */
export function ProgressRecord({ studentUserId, managed }: { studentUserId: string; managed: boolean }) {
  const progress = useStudentProgress(studentUserId);
  const mark = useMarkObjective(studentUserId);
  const unmark = useUnmarkObjective(studentUserId);
  const levels = progress.data?.levels ?? [];
  const current = levels.find((level) => level.current);
  const busy = mark.isPending || unmark.isPending;

  if (progress.isPending) return <p className="text-muted">Cargando…</p>;
  if (levels.every((level) => level.objectives.length === 0)) {
    return (
      <p className="text-muted">
        Tus niveles aún no tienen objetivos. Escríbelos en tu perfil, en «Niveles», y los marcarás aquí.
      </p>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      <ErrorNotice error={progress.error ?? mark.error ?? unmark.error} />
      {current?.completed ? (
        <p role="status" className="rounded-lg bg-surround-tint px-4 py-3 font-semibold text-surround-ink">
          Ha conseguido todos los objetivos de {current.levelName}.
        </p>
      ) : null}
      {levels.filter((level) => level.objectives.length > 0).map((level) => (
        <fieldset key={level.levelId} className="flex flex-col gap-2 rounded-xl border border-line bg-paper p-4"
          disabled={!managed || busy}>
          <legend className="flex items-center gap-2 px-1 font-semibold">
            {level.levelName} <Count level={level} />
            {level.current ? <Chip tone="court">Su nivel</Chip> : null}
          </legend>
          <ul className="flex flex-col gap-1">
            {level.objectives.map((objective) => (
              <li key={objective.id}>
                <label className="flex min-h-11 items-center gap-3">
                  <input type="checkbox" className="size-5 accent-court" checked={objective.achievedOn !== null}
                    onChange={(event) => (event.target.checked ? mark : unmark).mutate(objective.id)} />
                  <span className="flex-1">{objective.title}</span>
                  {objective.achievedOn ? (
                    <span className="text-sm text-muted">{formatShortDay(objective.achievedOn)}</span>
                  ) : null}
                </label>
              </li>
            ))}
          </ul>
        </fieldset>
      ))}
    </div>
  );
}

/** "3 de 5": achieved out of those shown for the level. */
export function Count({ level }: { level: LevelProgress }) {
  const achieved = level.objectives.filter((objective) => objective.achievedOn !== null).length;
  return <span className="text-sm font-normal text-muted">{achieved} de {level.objectives.length}</span>;
}
