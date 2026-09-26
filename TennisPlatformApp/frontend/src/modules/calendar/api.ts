import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";
import type { LocalDate } from "@/shared/time";

export type Calendar = components["schemas"]["Calendar"];
export type CalendarLesson = Calendar["lessons"][number];

export function useCalendar(from: LocalDate, to: LocalDate) {
  return useQuery({
    queryKey: ["calendar", from, to],
    queryFn: async () => unwrap(await api.GET("/calendar", { params: { query: { from, to } } })),
    // Moving to the next week keeps this one on screen until that one arrives.
    placeholderData: keepPreviousData,
  });
}
