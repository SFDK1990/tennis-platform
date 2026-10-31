import { useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";
import { formatTime, todayIn } from "@/shared/time";

export type FixedGroup = components["schemas"]["FixedGroup"];
export type GroupSeries = components["schemas"]["GroupSeries"];
export type FeeLine = components["schemas"]["FeeLine"];
export type FeeStatus = components["schemas"]["FeeStatus"];
export type Membership = components["schemas"]["Membership"];
export type FeePaymentMethod = NonNullable<components["schemas"]["RecordFeeRequest"]["paymentMethod"]>;
/** "2026-11": a month on the teacher's calendar, the way the API writes it. */
export type Month = string;

export const FEE_PAYMENT_METHODS: Record<FeePaymentMethod, string> = {
  CASH: "Efectivo",
  BIZUM: "Bizum",
  TRANSFER: "Transferencia",
};

export const FEE_STATUS: Record<FeeStatus, { label: string; tone: "success" | "neutral" | "warning" | "fault" }> = {
  PAID: { label: "Pagada", tone: "success" },
  WAIVED: { label: "No se cobra", tone: "neutral" },
  PENDING: { label: "Pendiente", tone: "warning" },
  OVERDUE: { label: "Sin pagar", tone: "fault" },
};

/** The month a day falls in, on the teacher's calendar. */
export function monthIn(zone: string): Month {
  return todayIn(zone).slice(0, 7);
}

export function addMonths(month: Month, months: number): Month {
  const [year, number] = month.split("-").map(Number);
  const date = new Date(Date.UTC(year, number - 1 + months, 1));
  return date.toISOString().slice(0, 7);
}

/** "noviembre de 2026". */
export function formatMonth(month: Month): string {
  return new Intl.DateTimeFormat("es-ES", { timeZone: "UTC", month: "long", year: "numeric" })
    .format(new Date(`${month}-01T00:00:00Z`));
}

/** "Los martes, 18:00 a 19:30": a weekly series by its next lesson; a series with none left says so. */
export function describeSeries(series: GroupSeries, zone: string): string {
  if (!series.nextStartsAt || !series.nextEndsAt) {
    return "Sin clases por delante";
  }
  const weekday = new Intl.DateTimeFormat("es-ES", { timeZone: zone, weekday: "long" }).format(new Date(series.nextStartsAt));
  return `Los ${weekday}${weekday.endsWith("s") ? "" : "s"}, ${formatTime(series.nextStartsAt, zone)} a ${formatTime(series.nextEndsAt, zone)}`;
}

/** The months somebody owes and has not paid, the past due ones first: what the warnings count. */
export function overdue(lines: FeeLine[]): FeeLine[] {
  return lines.filter((line) => line.status === "OVERDUE");
}

/** The groups a student can see and sign up for, without who is in them. */
export function useGroups() {
  return useQuery({
    queryKey: ["groups"],
    queryFn: async () => unwrap(await api.GET("/groups")).items,
  });
}

export function useTeacherGroups() {
  return useQuery({
    queryKey: ["groups", "teacher"],
    queryFn: async () => unwrap(await api.GET("/teacher/groups")).items,
  });
}

export function useCreateGroup() {
  return useMutation({
    mutationFn: async (body: components["schemas"]["CreateGroupRequest"]) =>
      unwrap(await api.POST("/teacher/groups", { body })),
  });
}

export function useUpdateGroup() {
  return useMutation({
    mutationFn: async ({ id, ...body }: { id: string } & components["schemas"]["UpdateGroupRequest"]) =>
      unwrap(await api.PATCH("/teacher/groups/{id}", { params: { path: { id } }, body })),
  });
}

export function useDeleteGroup() {
  return useMutation({
    mutationFn: async (id: string) =>
      unwrap(await api.DELETE("/teacher/groups/{id}", { params: { path: { id } } })),
  });
}

export function useAttachSeries() {
  return useMutation({
    mutationFn: async ({ groupId, seriesId }: { groupId: string; seriesId: string }) =>
      unwrap(await api.POST("/teacher/groups/{id}/series", { params: { path: { id: groupId } }, body: { seriesId } })),
  });
}

export function useAddMember() {
  return useMutation({
    mutationFn: async ({ groupId, studentUserId }: { groupId: string; studentUserId: string }) =>
      unwrap(await api.POST("/teacher/groups/{id}/members", {
        params: { path: { id: groupId } }, body: { studentUserId },
      })),
  });
}

export function useRemoveMember() {
  return useMutation({
    mutationFn: async ({ groupId, studentUserId }: { groupId: string; studentUserId: string }) =>
      unwrap(await api.DELETE("/teacher/groups/{id}/members/{userId}", {
        params: { path: { id: groupId, userId: studentUserId } },
      })),
  });
}

/** Every month of every member, or one month, or one student's (decision 3 of 43-fase28). */
export function useFees(filter: { month?: Month; studentUserId?: string } = {}) {
  return useQuery({
    queryKey: ["fees", filter.month ?? "all", filter.studentUserId ?? "all"],
    queryFn: async () => unwrap(await api.GET("/teacher/fees", { params: { query: filter } })),
  });
}

export function useRecordFee() {
  return useMutation({
    mutationFn: async (body: components["schemas"]["RecordFeeRequest"]) =>
      unwrap(await api.POST("/teacher/fees", { body })),
  });
}

export function useVoidFee() {
  return useMutation({
    mutationFn: async (id: string) =>
      unwrap(await api.POST("/teacher/fees/{id}/void", { params: { path: { id } } })),
  });
}

export function useChangeDueDay() {
  return useMutation({
    mutationFn: async (dueDay: number) => unwrap(await api.PUT("/teacher/fee-settings", { body: { dueDay } })),
  });
}

export function useMyMemberships() {
  return useQuery({
    queryKey: ["memberships", "mine"],
    queryFn: async () => unwrap(await api.GET("/me/memberships")).items,
  });
}

export function useJoinGroup() {
  return useMutation({
    mutationFn: async (groupId: string) =>
      unwrap(await api.POST("/me/groups/{id}/membership", { params: { path: { id: groupId } } })),
  });
}

export function useLeaveGroup() {
  return useMutation({
    mutationFn: async (groupId: string) =>
      unwrap(await api.DELETE("/me/groups/{id}/membership", { params: { path: { id: groupId } } })),
  });
}
