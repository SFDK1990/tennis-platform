import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";

export type MonthSummary = components["schemas"]["MonthSummary"];
export type MonthFigures = components["schemas"]["MonthFigures"];

/** "2026-10"; null asks for the current month. */
export type Month = string;

export function useMonthSummary(month: Month | null) {
  return useQuery({
    queryKey: ["summary", month],
    queryFn: async () => unwrap(await api.GET("/teacher/summary", { params: { query: month ? { month } : {} } })),
    placeholderData: keepPreviousData,
  });
}

/** The month next to this one, either way. */
export function shiftMonth(month: Month, by: number): Month {
  const [year, value] = month.split("-").map(Number);
  const index = year * 12 + (value - 1) + by;
  return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, "0")}`;
}

/** "octubre de 2026". */
export function formatMonth(month: Month): string {
  const [year, value] = month.split("-").map(Number);
  return new Intl.DateTimeFormat("es-ES", { timeZone: "UTC", month: "long", year: "numeric" })
    .format(new Date(Date.UTC(year, value - 1, 1)));
}

/** A whole percentage, or null when there is nothing to divide by. */
export function percent(part: number, whole: number): number | null {
  return whole === 0 ? null : Math.round((part * 100) / whole);
}

export function occupancy(figures: MonthFigures): number | null {
  return percent(figures.seatsTaken, figures.seatsOffered);
}

/** Only the seats the register was taken of: the unmarked ones count neither way. */
export function attendance(figures: MonthFigures): number | null {
  return percent(figures.attended, figures.attended + figures.noShow);
}
