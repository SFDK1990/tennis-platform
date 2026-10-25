"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";
import { CONTROL_CLASS } from "@/shared/ui/Field";

export type Level = components["schemas"]["Level"];

/**
 * The teacher's levels (Fase 23). In `shared` because lessons, students and bookings all show
 * them; only the teacher module changes them.
 */
export function useLevels() {
  return useQuery({
    queryKey: ["levels"],
    queryFn: async () => unwrap(await api.GET("/levels")).levels,
  });
}

/** The name of a level by its id, or null for none. */
export function useLevelName(): (levelId?: string | null) => string | null {
  const levels = useLevels();
  const names = new Map(levels.data?.map((level) => [level.id, level.name]));
  return (levelId) => (levelId ? names.get(levelId) ?? null : null);
}

interface LevelSelectProps {
  id: string;
  label: string;
  /** What no level means here: "Abierta a todos" for a lesson, "Sin nivel" for a student. */
  noneLabel: string;
  value: string | null;
  onChange: (levelId: string | null) => void;
}

export function LevelSelect({ id, label, noneLabel, value, onChange }: LevelSelectProps) {
  const levels = useLevels();
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold">{label}</label>
      <select id={id} value={value ?? ""} onChange={(event) => onChange(event.target.value || null)}
        className={CONTROL_CLASS}>
        <option value="">{noneLabel}</option>
        {levels.data?.map((level) => <option key={level.id} value={level.id}>{level.name}</option>)}
      </select>
    </div>
  );
}
