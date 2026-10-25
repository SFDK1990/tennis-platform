import { useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";

/**
 * The zone every date is shown in (22-fase11, decision 7). Readable by any signed-in user:
 * a student needs it as much as the teacher.
 */
export function useTeacherZone(): string | undefined {
  return useTeacherProfile().data?.timezone;
}

/** Who the teacher is: students see their name on the home screen. */
export function useTeacherProfile() {
  return useQuery({
    queryKey: ["teacher-profile"],
    queryFn: async () => unwrap(await api.GET("/teacher/profile")),
    staleTime: Infinity,
  });
}

/** The teacher changes their levels (Fase 23); everybody reads them through `@/shared/levels`. */
export function useCreateLevel() {
  return useMutation({
    mutationFn: async (name: string) => unwrap(await api.POST("/teacher/levels", { body: { name } })),
  });
}

export function useUpdateLevel() {
  return useMutation({
    mutationFn: async ({ id, name, position }: { id: string; name?: string; position?: number }) =>
      unwrap(await api.PATCH("/teacher/levels/{id}", { params: { path: { id } }, body: { name, position } })),
  });
}

export function useDeleteLevel() {
  return useMutation({
    mutationFn: async (id: string) => unwrap(await api.DELETE("/teacher/levels/{id}", { params: { path: { id } } })),
  });
}
