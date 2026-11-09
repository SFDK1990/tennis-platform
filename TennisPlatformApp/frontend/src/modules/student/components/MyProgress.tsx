"use client";

import { useMyProgress, type LevelProgress } from "@/modules/student/api";
import { formatShortDay } from "@/shared/time";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * «Mi progreso» (44-fase29-analisis-progreso.md, decisions 1 and 3): the objectives of the student's
 * level with a bar, and below, the earlier levels where they achieved something.
 */
export function MyProgress() {
  const progress = useMyProgress();
  const levels = progress.data?.levels ?? [];
  const current = levels.find((level) => level.current);
  const earlier = levels.filter((level) => !level.current);

  return (
    <section aria-labelledby="progress-heading" className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
      <h2 id="progress-heading" className="font-display text-2xl font-semibold leading-none">Mi progreso</h2>
      <ErrorNotice error={progress.error} />
      {progress.isPending ? <p className="text-muted">Cargando…</p> : null}
      {!progress.isPending && !current ? (
        <p className="text-muted">Tu profesor aún no te ha puesto nivel.</p>
      ) : null}
      {current ? <LevelBlock level={current} headline /> : null}
      {earlier.map((level) => <LevelBlock key={level.levelId} level={level} />)}
    </section>
  );
}

function LevelBlock({ level, headline = false }: { level: LevelProgress; headline?: boolean }) {
  const achieved = level.objectives.filter((objective) => objective.achievedOn !== null).length;
  const total = level.objectives.length;
  const title = headline ? `Tu nivel: ${level.levelName}` : level.levelName;

  if (total === 0) {
    return (
      <div className="flex flex-col gap-1">
        <h3 className="font-semibold">{title}</h3>
        <p className="text-sm text-muted">Tu profesor aún no ha puesto objetivos para tu nivel.</p>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="font-semibold">{title}</h3>
        <span className="text-sm text-muted">{achieved} de {total}</span>
      </div>
      {headline ? (
        <progress value={achieved} max={total} aria-label={`${achieved} de ${total} objetivos conseguidos`}
          className="h-2 w-full overflow-hidden rounded-full accent-link" />
      ) : null}
      {level.completed && headline ? (
        <p className="text-sm font-semibold text-surround-ink">¡Has conseguido todos los objetivos de tu nivel!</p>
      ) : null}
      <ul className="flex flex-col gap-1">
        {level.objectives.map((objective) => (
          <li key={objective.id} className="flex min-h-9 items-center gap-2">
            <Icon name={objective.achievedOn ? "check" : "circle"}
              className={`size-5 shrink-0 ${objective.achievedOn ? "text-surround-ink" : "text-muted"}`} />
            <span className={`flex-1 ${objective.achievedOn ? "" : "text-muted"}`}>
              {objective.title}
              <span className="sr-only">{objective.achievedOn ? ", conseguido" : ", pendiente"}</span>
            </span>
            {objective.achievedOn ? (
              <span className="text-sm text-muted">{formatShortDay(objective.achievedOn)}</span>
            ) : null}
          </li>
        ))}
      </ul>
    </div>
  );
}
