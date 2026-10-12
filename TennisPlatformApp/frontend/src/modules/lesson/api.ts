import { useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";

export type Lesson = components["schemas"]["Lesson"];
export type CreateLesson = components["schemas"]["CreateLessonRequest"];

export function useLesson(id: string) {
  return useQuery({
    queryKey: ["lesson", id],
    queryFn: async () => unwrap(await api.GET("/lessons/{id}", { params: { path: { id } } })),
  });
}

export function useCreateLesson() {
  return useMutation({
    mutationFn: async (body: CreateLesson) => unwrap(await api.POST("/teacher/lessons", { body })),
  });
}

export type EditLesson = components["schemas"]["EditLessonRequest"];

export function useEditLesson(id: string) {
  return useMutation({
    mutationFn: async (body: EditLesson) =>
      unwrap(await api.PATCH("/teacher/lessons/{id}", { params: { path: { id } }, body })),
  });
}

/**
 * The teacher cancels through their route, with the reason the students will read; the
 * administrator through theirs, and the bookings say who.
 */
export function useCancelLesson(as: "teacher" | "admin" = "teacher") {
  return useMutation({
    mutationFn: async ({ id, reason }: { id: string; reason?: string }) =>
      unwrap(as === "admin"
        ? await api.POST("/admin/lessons/{id}/cancel", { params: { path: { id } } })
        : await api.POST("/teacher/lessons/{id}/cancel", { params: { path: { id } }, body: { reason } })),
  });
}

export type CancelDay = components["schemas"]["CancelDayRequest"];

/** Every lesson of a day in the teacher's zone, cancelled whole or not at all. */
export function useCancelDay() {
  return useMutation({
    mutationFn: async (body: CancelDay) => unwrap(await api.POST("/teacher/lessons/cancellations", { body })),
  });
}
