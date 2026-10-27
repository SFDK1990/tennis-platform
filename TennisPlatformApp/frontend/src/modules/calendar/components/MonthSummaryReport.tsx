"use client";

import { useState } from "react";
import {
  attendance, formatMonth, occupancy, shiftMonth, useMonthSummary, type MonthFigures, type MonthSummary,
} from "@/modules/calendar/summary";
import { useLevels } from "@/shared/levels";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * The teacher's month (38-fase25): what was given, how full, who came and who was active, next to
 * the month before, and the same split by level.
 */
export function MonthSummaryReport() {
  const [month, setMonth] = useState<string | null>(null);
  const summary = useMonthSummary(month);
  // The current month is the server's, on the teacher's clock: null always asks for it.
  const latest = useMonthSummary(null).data?.month;
  const shown = summary.data?.month;

  if (summary.error) {
    return <ErrorNotice error={summary.error} />;
  }
  if (!summary.data || !shown) {
    return <p className="text-muted">Cargando…</p>;
  }
  const isCurrent = shown === latest;
  const next = shiftMonth(shown, 1);
  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-3">
        <Button variant="quiet" onClick={() => setMonth(shiftMonth(shown, -1))} aria-label="Mes anterior"
          className="max-sm:px-3">
          <Icon name="chevronLeft" /><span className="max-sm:hidden">Anterior</span>
        </Button>
        <p className="text-center font-display text-xl font-semibold first-letter:uppercase">
          {formatMonth(shown)}{isCurrent ? <span className="block text-sm font-normal text-muted">hasta hoy</span> : null}
        </p>
        <Button variant="quiet" disabled={isCurrent} aria-label="Mes siguiente" className="max-sm:px-3"
          onClick={() => setMonth(next === latest ? null : next)}>
          <span className="max-sm:hidden">Siguiente</span><Icon name="chevronRight" />
        </Button>
      </div>
      <Figures data={summary.data} />
      <Levels data={summary.data} />
    </div>
  );
}

function Figures({ data }: { data: MonthSummary }) {
  const { current, previous } = data;
  const occupied = occupancy(current);
  const came = attendance(current);
  return (
    <section aria-labelledby="month-figures" className="overflow-hidden rounded-xl border border-line bg-paper">
      <h2 id="month-figures" className="sr-only">Los números del mes</h2>
      <dl className="grid gap-px bg-line-soft sm:grid-cols-2 lg:grid-cols-4">
        <Figure label="Clases dadas" value={String(current.lessonsGiven)}
          detail={`${current.individualLessons} ${current.individualLessons === 1 ? "individual" : "individuales"} y ${current.groupLessons} de grupo`}
          before={String(previous.lessonsGiven)} />
        <Figure label="Ocupación" value={occupied === null ? "—" : `${occupied} %`}
          detail={`${current.seatsTaken} de ${current.seatsOffered} ${current.seatsOffered === 1 ? "plaza" : "plazas"}`}
          before={formatPercent(occupancy(previous))} />
        <Figure label="Asistencia" value={came === null ? "—" : `${came} %`}
          detail={current.unmarked === 0 ? "Con lista en todas las plazas"
            : `${current.unmarked} ${current.unmarked === 1 ? "plaza" : "plazas"} sin pasar lista`}
          before={formatPercent(attendance(previous))} />
        <Figure label="Alumnos activos" value={String(current.activeStudents)}
          detail="Con al menos una clase en el mes" before={String(previous.activeStudents)} />
      </dl>
      <p className="border-t border-line-soft px-5 py-3 text-sm text-muted">
        {cancelled(current)} El mes anterior: {cancelled(previous).toLowerCase()}
      </p>
    </section>
  );
}

function Figure({ label, value, detail, before }: { label: string; value: string; detail: string; before: string }) {
  return (
    <div className="flex flex-col gap-1 bg-paper px-5 py-4">
      <dt className="text-sm font-semibold text-muted">{label}</dt>
      <dd className="font-display text-4xl font-semibold leading-none">{value}</dd>
      <dd className="text-sm">{detail}</dd>
      <dd className="text-sm text-muted">Mes anterior: {before}</dd>
    </div>
  );
}

function Levels({ data }: { data: MonthSummary }) {
  const levels = useLevels().data ?? [];
  const position = new Map(levels.map((level) => [level.id, level.position]));
  const name = new Map(levels.map((level) => [level.id, level.name]));
  const rows = data.levels.toSorted((a, b) =>
    (a.levelId ? position.get(a.levelId) ?? 99 : 100) - (b.levelId ? position.get(b.levelId) ?? 99 : 100));
  if (rows.length === 0) {
    return null;
  }
  return (
    <section aria-labelledby="by-level" className="flex flex-col gap-3">
      <h2 id="by-level" className="font-display text-2xl font-semibold">Por nivel</h2>
      <div className="overflow-x-auto rounded-xl border border-line bg-paper">
        <table className="w-full min-w-[34rem] text-left">
          <thead className="border-b border-line text-sm text-muted">
            <tr>
              <th scope="col" className="px-4 py-2.5 font-semibold">Nivel</th>
              <th scope="col" className="px-4 py-2.5 text-right font-semibold">Clases</th>
              <th scope="col" className="px-4 py-2.5 text-right font-semibold">Ocupación</th>
              <th scope="col" className="px-4 py-2.5 text-right font-semibold">Asistencia</th>
              <th scope="col" className="px-4 py-2.5 text-right font-semibold">Activos</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line-soft">
            {rows.map((row) => (
              <tr key={row.levelId ?? "open"}>
                <th scope="row" className="px-4 py-3 font-semibold">
                  {row.levelId ? name.get(row.levelId) ?? "Nivel" : "Abiertas a todos"}
                </th>
                <Cell now={String(row.current.lessonsGiven)} before={String(row.previous.lessonsGiven)} />
                <Cell now={formatPercent(occupancy(row.current))} before={formatPercent(occupancy(row.previous))} />
                <Cell now={formatPercent(attendance(row.current))} before={formatPercent(attendance(row.previous))} />
                <Cell now={String(row.current.activeStudents)} before={String(row.previous.activeStudents)} />
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="text-sm text-muted">Entre paréntesis, el mes anterior. Quien tiene clases de dos niveles cuenta en los dos.</p>
    </section>
  );
}

function Cell({ now, before }: { now: string; before: string }) {
  return (
    <td className="px-4 py-3 text-right tabular-nums">
      {now} <span className="text-sm text-muted">({before})</span>
    </td>
  );
}

function formatPercent(value: number | null): string {
  return value === null ? "—" : `${value} %`;
}

function cancelled(figures: MonthFigures): string {
  if (figures.cancelledLessons === 0) {
    return "Ninguna clase cancelada.";
  }
  const total = figures.cancelledLessons === 1 ? "1 clase cancelada" : `${figures.cancelledLessons} clases canceladas`;
  return figures.cancelledAtShortNotice === 0
    ? `${total}.`
    : `${total}, ${figures.cancelledAtShortNotice} con menos de 24 horas.`;
}
