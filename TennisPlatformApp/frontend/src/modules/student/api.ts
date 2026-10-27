import { useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";
import { squareJpeg, toDataUrl } from "@/shared/photo";

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

/** Whether each managed student has a photo the teacher may ask for (Fase 25.5). */
export function useStudentHasPhoto(): (userId: string) => boolean {
  const students = useManagedStudents();
  const withPhoto = new Set(students.data?.items.filter((student) => student.hasPhoto).map((student) => student.userId));
  return (userId) => withPhoto.has(userId);
}

export type StudentDetail = components["schemas"]["StudentDetail"];
export type StudentRecord = components["schemas"]["StudentRecordRequest"];

/** The full record of one managed student, restricted data included (only their teacher reads it). */
export function useStudentDetail(userId: string) {
  return useQuery({
    queryKey: ["students", userId],
    queryFn: async () => unwrap(await api.GET("/teacher/students/{userId}", { params: { path: { userId } } })),
  });
}

/** Level, goals and private notes, replaced together (Fase 23). */
export function useUpdateStudentRecord(userId: string) {
  return useMutation({
    mutationFn: async (body: StudentRecord) =>
      unwrap(await api.PUT("/teacher/students/{userId}/record", { params: { path: { userId } }, body })),
  });
}

/**
 * A managed student's photo as a data URL (Fase 25.5), asked for with the teacher's token: an
 * <img> could not carry it. Only asked for when the student has one.
 */
export function useStudentPhoto(userId: string, hasPhoto: boolean) {
  return useQuery({
    queryKey: ["photo", userId],
    queryFn: async () => toDataUrl(unwrap(await api.GET("/teacher/students/{userId}/photo",
      { params: { path: { userId } }, parseAs: "blob" })) as unknown as Blob),
    enabled: hasPhoto,
    staleTime: Infinity,
  });
}

/** The signed-in student's own photo. */
export function useMyPhoto(hasPhoto: boolean) {
  return useQuery({
    queryKey: ["photo", "mine"],
    queryFn: async () => toDataUrl(unwrap(await api.GET("/me/photo", { parseAs: "blob" })) as unknown as Blob),
    enabled: hasPhoto,
  });
}

/** Crops and reduces the picture in the browser first: a phone's 4 MB photo leaves as some 40 KB. */
export function useUploadPhoto() {
  return useMutation({
    mutationFn: async (file: File) => {
      const image = await squareJpeg(file);
      return unwrap(await api.PUT("/me/photo", {
        body: image as unknown as string,
        bodySerializer: (body) => body as unknown as BodyInit,
        headers: { "Content-Type": "image/jpeg" },
      }));
    },
  });
}

export function useRemovePhoto() {
  return useMutation({
    mutationFn: async () => unwrap(await api.DELETE("/me/photo")),
  });
}
