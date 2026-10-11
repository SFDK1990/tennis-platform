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

/** The teacher cancels through their route; the administrator through theirs, and the bookings say who. */
export function useCancelLesson(as: "teacher" | "admin" = "teacher") {
  return useMutation({
    mutationFn: async (id: string) =>
      unwrap(as === "admin"
        ? await api.POST("/admin/lessons/{id}/cancel", { params: { path: { id } } })
        : await api.POST("/teacher/lessons/{id}/cancel", { params: { path: { id } } })),
  });
}
