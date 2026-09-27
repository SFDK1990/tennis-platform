import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSyncExternalStore } from "react";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";
import {
  getSession, refreshAccessToken, signOut, startSession, subscribeToSession, type Session,
} from "@/shared/api/session";

export type Me = components["schemas"]["MeResponse"];
export type Role = components["schemas"]["Role"];
type UpdateMe = components["schemas"]["UpdateMeRequest"];

const RESTORING: Session = { status: "restoring", token: null };

/** Where each role starts. */
export function homeFor(role: Role): string {
  switch (role) {
    case "TEACHER":
      return "/teacher";
    case "STUDENT":
      return "/home";
    case "ADMIN":
      return "/admin";
    default:
      return "/profile";
  }
}

export function useSession(): Session {
  return useSyncExternalStore(subscribeToSession, getSession, () => RESTORING);
}

export function useMe() {
  const { status } = useSession();
  return useQuery({
    queryKey: ["me"],
    queryFn: async () => unwrap(await api.GET("/me")),
    enabled: status === "signed-in",
  });
}

export function useLogin() {
  return useMutation({
    mutationFn: async (body: { email: string; password: string }) => {
      const result = unwrap(await api.POST("/auth/login", { body }));
      startSession(result.accessToken);
      return result.user;
    },
  });
}

export function useSignOut() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: signOut,
    // Nothing of the previous account may survive into the next one on this browser.
    onSettled: () => queryClient.clear(),
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: async (body: { email: string; password: string }) => unwrap(await api.POST("/auth/register", { body })),
  });
}

export function useVerifyEmail() {
  return useMutation({
    mutationFn: async (token: string) => {
      unwrap(await api.POST("/auth/verify-email", { body: { token } }));
      // "Verified" travels inside the access token: a signed-in user needs a new one to book.
      if (getSession().status === "signed-in") {
        await refreshAccessToken();
      }
    },
  });
}

export function useResendVerification() {
  return useMutation({
    mutationFn: async () => unwrap(await api.POST("/auth/verification-email")),
  });
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: async (email: string) => unwrap(await api.POST("/auth/forgot-password", { body: { email } })),
  });
}

export function useResetPassword() {
  return useMutation({
    mutationFn: async (body: { token: string; newPassword: string }) =>
      unwrap(await api.POST("/auth/reset-password", { body })),
  });
}

export function useUpdateMe() {
  return useMutation({
    mutationFn: async (body: UpdateMe) => unwrap(await api.PATCH("/me", { body })),
  });
}
