import { useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";

export type StudentSummary = components["schemas"]["StudentSummary"];

export function useManagedStudents() {
  return useQuery({
    queryKey: ["students"],
    queryFn: async () => unwrap(await api.GET("/teacher/students", { params: { query: { size: 100 } } })),
  });
}

/** Only by the full address: a partial search would let anyone list who is registered. */
export function useStudentLookup(email: string | null) {
  return useQuery({
    queryKey: ["students", "lookup", email],
    queryFn: async () => unwrap(await api.GET("/teacher/students/lookup", { params: { query: { email: email! } } })),
    enabled: email !== null,
    retry: false,
  });
}

export function useManageStudent() {
  return useMutation({
    mutationFn: async (userId: string) =>
      unwrap(await api.POST("/teacher/students/{userId}/manage", { params: { path: { userId } } })),
  });
}

export function useStopManaging() {
  return useMutation({
    mutationFn: async (userId: string) =>
      unwrap(await api.DELETE("/teacher/students/{userId}/manage", { params: { path: { userId } } })),
  });
}

/** Bookings carry only a student id; this turns it into the name the teacher knows. */
export function useStudentNames(): (userId: string) => string {
  const students = useManagedStudents();
  const names = new Map(students.data?.items.map((student) => [student.userId, student.fullName]));
  return (userId) => names.get(userId) ?? "Alumno";
}
