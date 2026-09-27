import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";

export type PlatformConfiguration = components["schemas"]["PlatformConfiguration"];
export type AdminUser = components["schemas"]["AdminUserSummary"];
type Role = components["schemas"]["Role"];
type UserStatus = components["schemas"]["UserStatus"];

export interface UserFilters {
  role?: Role;
  status?: UserStatus;
  query?: string;
}

export function useConfiguration() {
  return useQuery({
    queryKey: ["admin", "configuration"],
    queryFn: async () => unwrap(await api.GET("/admin/configuration")),
  });
}

export function useUpdateConfiguration() {
  return useMutation({
    mutationFn: async (body: components["schemas"]["UpdateConfigurationRequest"]) =>
      unwrap(await api.PATCH("/admin/configuration", { body })),
  });
}

export function useUsers(filters: UserFilters, page: number) {
  return useQuery({
    queryKey: ["admin", "users", filters, page],
    queryFn: async () => unwrap(await api.GET("/admin/users", { params: { query: { ...filters, page, size: 20 } } })),
    placeholderData: keepPreviousData,
  });
}

export function useChangeUserStatus() {
  return useMutation({
    mutationFn: async ({ id, status }: { id: string; status: "ACTIVE" | "DISABLED" }) =>
      unwrap(await api.PATCH("/admin/users/{id}/status", { params: { path: { id } }, body: { status } })),
  });
}
