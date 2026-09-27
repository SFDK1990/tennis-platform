import { useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";
import type { LocalDate } from "@/shared/time";

export type WeeklyRule = components["schemas"]["WeeklyAvailabilityRule"];
export type WeeklyRuleInput = components["schemas"]["WeeklyAvailabilityRequest"]["rules"][number];
export type AvailabilityException = components["schemas"]["AvailabilityException"];
export type NewException = components["schemas"]["CreateAvailabilityExceptionRequest"];

export function useAvailability(from: LocalDate, to: LocalDate) {
  return useQuery({
    queryKey: ["availability", from, to],
    queryFn: async () => unwrap(await api.GET("/teacher/availability", { params: { query: { from, to } } })),
  });
}

/** Replaces the whole weekly set: a rule left out stops existing (openapi.yaml). */
export function useSaveWeeklyRules() {
  return useMutation({
    mutationFn: async (rules: WeeklyRuleInput[]) =>
      unwrap(await api.PUT("/teacher/availability/weekly", { body: { rules } })),
  });
}

export function useCreateException() {
  return useMutation({
    mutationFn: async (body: NewException) =>
      unwrap(await api.POST("/teacher/availability/exceptions", { body })),
  });
}

export function useDeleteException() {
  return useMutation({
    mutationFn: async (id: string) =>
      unwrap(await api.DELETE("/teacher/availability/exceptions/{id}", { params: { path: { id } } })),
  });
}
