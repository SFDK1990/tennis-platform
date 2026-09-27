"use client";

import { useState } from "react";
import { useSaveWeeklyRules, type WeeklyRule, type WeeklyRuleInput } from "@/modules/availability/api";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

const DAYS: { value: WeeklyRuleInput["dayOfWeek"]; label: string }[] = [
  { value: "MONDAY", label: "Lunes" },
  { value: "TUESDAY", label: "Martes" },
  { value: "WEDNESDAY", label: "Miércoles" },
  { value: "THURSDAY", label: "Jueves" },
  { value: "FRIDAY", label: "Viernes" },
  { value: "SATURDAY", label: "Sábado" },
  { value: "SUNDAY", label: "Domingo" },
];

interface Row extends WeeklyRuleInput {
  key: string;
}

const hhmm = (time: string) => time.slice(0, 5);

/**
 * Read, edit, write the whole set back. activeFrom and activeUntil are carried untouched, or
 * saving would silently drop them (the PUT replaces everything).
 */
export function WeeklyRulesEditor({ rules }: { rules: WeeklyRule[] }) {
  const save = useSaveWeeklyRules();
  const [rows, setRows] = useState<Row[]>(() =>
    rules.map((rule) => ({ ...rule, key: rule.id, startTime: hhmm(rule.startTime), endTime: hhmm(rule.endTime) })),
  );

  const change = (key: string, patch: Partial<Row>) =>
    setRows((current) => current.map((row) => (row.key === key ? { ...row, ...patch } : row)));

  function add() {
    setRows((current) => [
      ...current,
      { key: crypto.randomUUID(), dayOfWeek: "MONDAY", startTime: "09:00", endTime: "13:00", activeFrom: null, activeUntil: null },
    ]);
  }

  return (
    <div className="flex flex-col gap-4">
      {rows.length === 0 ? <p className="text-muted">Sin horario fijo. Añade una franja por cada tramo que trabajas.</p> : null}
      <ul className="flex flex-col gap-2">
        {rows.map((row) => (
          <li key={row.key} className="flex flex-wrap items-end gap-3 rounded-xl border border-line bg-paper px-4 py-3">
            <label className="flex flex-col gap-1 text-sm font-semibold">
              Día
              <select value={row.dayOfWeek} onChange={(e) => change(row.key, { dayOfWeek: e.target.value as Row["dayOfWeek"] })}
                className={CONTROL_CLASS}>
                {DAYS.map((day) => <option key={day.value} value={day.value}>{day.label}</option>)}
              </select>
            </label>
            <label className="flex flex-col gap-1 text-sm font-semibold">
              Desde
              <input type="time" step={1800} value={row.startTime} onChange={(e) => change(row.key, { startTime: e.target.value })}
                className={CONTROL_CLASS} />
            </label>
            <label className="flex flex-col gap-1 text-sm font-semibold">
              Hasta
              <input type="time" step={1800} value={row.endTime} onChange={(e) => change(row.key, { endTime: e.target.value })}
                className={CONTROL_CLASS} />
            </label>
            <Button variant="quiet" onClick={() => setRows((current) => current.filter((r) => r.key !== row.key))}>
              Quitar
            </Button>
          </li>
        ))}
      </ul>
      <ErrorNotice error={save.error} />
      {save.isSuccess ? <Notice tone="success">Horario guardado.</Notice> : null}
      <div className="flex gap-3">
        <Button variant="quiet" onClick={add}>Añadir franja</Button>
        <Button pending={save.isPending} onClick={() => save.mutate(rows.map(({ dayOfWeek, startTime, endTime, activeFrom, activeUntil }) =>
          ({ dayOfWeek, startTime, endTime, activeFrom, activeUntil })))}>
          Guardar horario
        </Button>
      </div>
    </div>
  );
}
