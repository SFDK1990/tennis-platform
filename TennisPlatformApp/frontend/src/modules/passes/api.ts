import { useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";
import { formatEuros } from "@/shared/money";
import { formatShortDay, type LocalDate } from "@/shared/time";

export type PassType = components["schemas"]["PassType"];
export type Pass = components["schemas"]["Pass"];
export type LessonKind = PassType["lessonType"];
export type PaymentMethod = components["schemas"]["PaymentMethod"];
export type RecordedPaymentMethod = components["schemas"]["RecordPassRequest"]["paymentMethod"];
export type Purchase = components["schemas"]["PassPurchase"];

export const KIND_PLURAL: Record<LessonKind, string> = { GROUP: "de grupo", INDIVIDUAL: "particulares" };

/** What the teacher can write down by hand; ONLINE only the app sets (42-fase27.5). */
export const RECORDED_PAYMENT_METHODS: Record<RecordedPaymentMethod, string> = {
  CASH: "Efectivo",
  BIZUM: "Bizum",
  TRANSFER: "Transferencia",
};

export const PAYMENT_METHODS: Record<PaymentMethod, string> = {
  ...RECORDED_PAYMENT_METHODS,
  ONLINE: "Pagado en la app",
};

export { formatEuros } from "@/shared/money";

/** "10 clases de grupo, 3 meses", the way a type reads in a list or a select. */
export function describeType(type: PassType): string {
  const lessons = `${type.lessons} ${type.lessons === 1 ? "clase" : "clases"} ${KIND_PLURAL[type.lessonType]}`;
  const validity = type.validityMonths
    ? `${type.validityMonths} ${type.validityMonths === 1 ? "mes" : "meses"}`
    : "sin caducidad";
  return `${lessons}, ${formatEuros(type.priceCents)}, ${validity}`;
}

/**
 * What a student can still book, by kind of lesson: the lessons left on the passes that are
 * active today, and the last day the first of them to expire lasts (decision 4 of
 * 41-fase27-analisis-bonos.md). The backend decides; this only says it in words.
 */
export interface Balance {
  kind: LessonKind;
  remaining: number;
  until: LocalDate | null;
}

export function balancesOf(passes: Pass[]): Balance[] {
  const kinds: LessonKind[] = ["GROUP", "INDIVIDUAL"];
  return kinds.flatMap((kind) => {
    const ofKind = passes.filter((pass) => pass.lessonType === kind && pass.status !== "VOIDED");
    if (ofKind.length === 0) return [];
    const active = ofKind.filter((pass) => pass.status === "ACTIVE");
    const until = active.map((pass) => pass.validUntil).filter((day): day is string => day !== null).toSorted()[0] ?? null;
    return [{ kind, remaining: active.reduce((sum, pass) => sum + pass.remaining, 0), until }];
  });
}

/** "Te quedan 4 clases de grupo, hasta el 15 ene." */
export function describeBalance(balance: Balance): string {
  const lessons = `${balance.remaining} ${balance.remaining === 1 ? "clase" : "clases"} ${KIND_PLURAL[balance.kind]}`;
  return balance.until ? `${lessons}, hasta el ${formatShortDay(balance.until)}` : lessons;
}

/** Everybody reads the types and whether a pass is needed to book. */
export function usePassTypes() {
  return useQuery({
    queryKey: ["pass-types"],
    queryFn: async () => unwrap(await api.GET("/pass-types")),
  });
}

export function useCreatePassType() {
  return useMutation({
    mutationFn: async (body: components["schemas"]["CreatePassTypeRequest"]) =>
      unwrap(await api.POST("/teacher/pass-types", { body })),
  });
}

export function useUpdatePassType() {
  return useMutation({
    mutationFn: async ({ id, ...body }: { id: string } & components["schemas"]["UpdatePassTypeRequest"]) =>
      unwrap(await api.PATCH("/teacher/pass-types/{id}", { params: { path: { id } }, body })),
  });
}

export function useDeletePassType() {
  return useMutation({
    mutationFn: async (id: string) =>
      unwrap(await api.DELETE("/teacher/pass-types/{id}", { params: { path: { id } } })),
  });
}

export function useRequirePasses() {
  return useMutation({
    mutationFn: async (required: boolean) =>
      unwrap(await api.PUT("/teacher/pass-settings", { body: { required } })),
  });
}

/** Selling passes in the app (42-fase27.5, decision 2). */
export function useSellOnline() {
  return useMutation({
    mutationFn: async (onlinePayment: boolean) =>
      unwrap(await api.PUT("/teacher/pass-settings", { body: { onlinePayment } })),
  });
}

/** Starts a purchase; the answer carries Stripe's page, where the student goes to pay. */
export function useBuyPass() {
  return useMutation({
    mutationFn: async (passTypeId: string) =>
      unwrap(await api.POST("/me/pass-purchases", { body: { passTypeId } })),
  });
}

/**
 * A purchase after coming back from Stripe's page. The pass is created when Stripe's notice
 * reaches the server, which can be a few seconds after the student is back, so it asks again
 * while the purchase is still pending.
 */
export function usePurchase(id: string) {
  return useQuery({
    queryKey: ["pass-purchases", id],
    queryFn: async () => unwrap(await api.GET("/me/pass-purchases/{id}", { params: { path: { id } } })),
    refetchInterval: (query) => (query.state.data?.status === "PENDING" ? 2000 : false),
  });
}

/** The passes of the teacher's students, or of one of them. */
export function useTeacherPasses(studentUserId?: string) {
  return useQuery({
    queryKey: ["passes", "teacher", studentUserId ?? "all"],
    queryFn: async () => unwrap(await api.GET("/teacher/passes", {
      params: { query: studentUserId ? { studentUserId } : {} },
    })),
  });
}

export function useRecordPass(studentUserId: string) {
  return useMutation({
    mutationFn: async (body: components["schemas"]["RecordPassRequest"]) =>
      unwrap(await api.POST("/teacher/students/{userId}/passes", { params: { path: { userId: studentUserId } }, body })),
  });
}

export function useVoidPass() {
  return useMutation({
    mutationFn: async (id: string) =>
      unwrap(await api.POST("/teacher/passes/{id}/void", { params: { path: { id } } })),
  });
}

export function useMyPasses() {
  return useQuery({
    queryKey: ["passes", "mine"],
    queryFn: async () => unwrap(await api.GET("/me/passes")),
  });
}
