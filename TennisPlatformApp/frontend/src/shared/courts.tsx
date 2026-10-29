"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";
import { CONTROL_CLASS } from "@/shared/ui/Field";

export type Court = components["schemas"]["Court"];

/**
 * The school's courts (Fase 25.6), in their order: the first is where a lesson goes by default.
 * In `shared` because lessons, bookings and the calendar all name them; only `lesson` renames them.
 */
export function useCourts() {
  return useQuery({
    queryKey: ["courts"],
    queryFn: async () => unwrap(await api.GET("/courts")).courts,
    staleTime: 5 * 60_000,
  });
}

/** The name of a court by its id, or null while the courts load. */
export function useCourtName(): (courtId?: string | null) => string | null {
  const courts = useCourts();
  const names = new Map(courts.data?.map((court) => [court.id, court.name]));
  return (courtId) => (courtId ? names.get(courtId) ?? null : null);
}

interface CourtSelectProps {
  id: string;
  label: string;
  /** Null shows the first court, which is where the lesson goes when none is chosen. */
  value: string | null;
  onChange: (courtId: string) => void;
}

export function CourtSelect({ id, label, value, onChange }: CourtSelectProps) {
  const courts = useCourts();
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold">{label}</label>
      <select id={id} value={value ?? courts.data?.[0]?.id ?? ""} onChange={(event) => onChange(event.target.value)}
        className={CONTROL_CLASS}>
        {courts.data?.map((court) => <option key={court.id} value={court.id}>{court.name}</option>)}
      </select>
    </div>
  );
}
