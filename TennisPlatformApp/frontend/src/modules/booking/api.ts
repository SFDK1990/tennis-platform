import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";

export type Booking = components["schemas"]["Booking"];
export type AttendanceMark = components["schemas"]["MarkAttendanceRequest"]["entries"][number];
export type WaitingPlace = components["schemas"]["WaitingPlace"];

const CANCELLATION_WINDOW_MS = 24 * 60 * 60 * 1000;

/** The same 24 hours the backend enforces; here it only decides what to warn about. */
export function insideCancellationWindow(startsAt: string, now: number): boolean {
  return new Date(startsAt).getTime() - now < CANCELLATION_WINDOW_MS;
}

export function useMyBookings(page: number) {
  return useQuery({
    queryKey: ["bookings", "mine", page],
    queryFn: async () => unwrap(await api.GET("/bookings", { params: { query: { page, size: 20 } } })),
    placeholderData: keepPreviousData,
  });
}

export function useLessonBookings(lessonId: string) {
  return useQuery({
    queryKey: ["bookings", "lesson", lessonId],
    queryFn: async () => unwrap(await api.GET("/bookings", { params: { query: { lessonId, size: 100 } } })),
  });
}

export function useBookLesson() {
  return useMutation({
    mutationFn: async (lessonId: string) =>
      unwrap(await api.POST("/lessons/{id}/bookings", { params: { path: { id: lessonId } } })),
  });
}

export function useCancelBooking() {
  return useMutation({
    mutationFn: async (bookingId: string) =>
      unwrap(await api.POST("/bookings/{id}/cancel", { params: { path: { id: bookingId } } })),
  });
}

export function useMarkAttendance(lessonId: string) {
  return useMutation({
    mutationFn: async (entries: AttendanceMark[]) =>
      unwrap(await api.POST("/teacher/lessons/{id}/attendance", { params: { path: { id: lessonId } }, body: { entries } })),
  });
}

/** The student's open waiting lists (Fase 21). One query, shared by every lesson on screen. */
export function useMyWaitingList() {
  return useQuery({
    queryKey: ["waiting-list", "mine"],
    queryFn: async () => unwrap(await api.GET("/waiting-list")).items,
  });
}

export function useJoinWaitingList() {
  return useMutation({
    mutationFn: async (lessonId: string) =>
      unwrap(await api.POST("/lessons/{id}/waiting-list", { params: { path: { id: lessonId } } })),
  });
}

export function useLeaveWaitingList() {
  return useMutation({
    mutationFn: async (lessonId: string) =>
      unwrap(await api.DELETE("/lessons/{id}/waiting-list", { params: { path: { id: lessonId } } })),
  });
}

export function useLessonWaitingList(lessonId: string) {
  return useQuery({
    queryKey: ["waiting-list", "lesson", lessonId],
    queryFn: async () =>
      unwrap(await api.GET("/teacher/lessons/{id}/waiting-list", { params: { path: { id: lessonId } } })).items,
  });
}

/** The teacher's view of one student's bookings with them, newest lesson first (Fase 23). */
export function useStudentHistory(studentUserId: string) {
  return useQuery({
    queryKey: ["bookings", "student", studentUserId],
    queryFn: async () => unwrap(await api.GET("/bookings", { params: { query: { studentUserId, size: 100 } } })),
  });
}
