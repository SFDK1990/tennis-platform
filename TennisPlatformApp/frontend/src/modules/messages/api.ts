import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/shared/api/client";
import { unwrap } from "@/shared/api/errors";
import type { components } from "@/shared/api/schema";

export type SentMessage = components["schemas"]["SentMessage"];
export type ReceivedMessage = components["schemas"]["ReceivedMessage"];
export type MessageDraft = components["schemas"]["SendMessageRequest"];

/** The limits of the contract, so the form stops where the backend would refuse. */
export const MAX_SUBJECT = 120;
export const MAX_BODY = 4000;

/** What the teacher has sent, newest first (Fase 24). */
export function useSentMessages(page: number) {
  return useQuery({
    queryKey: ["messages", "sent", page],
    queryFn: async () => unwrap(await api.GET("/teacher/messages", { params: { query: { page, size: 20 } } })),
    placeholderData: keepPreviousData,
  });
}

/** The signed-in student's board: only what was sent to them. */
export function useMyMessages(page = 0, size = 20) {
  return useQuery({
    queryKey: ["messages", "mine", page, size],
    queryFn: async () => unwrap(await api.GET("/messages", { params: { query: { page, size } } })),
    placeholderData: keepPreviousData,
  });
}

export function useSendMessage() {
  return useMutation({
    mutationFn: async (draft: MessageDraft) => unwrap(await api.POST("/teacher/messages", { body: draft })),
  });
}

export function useDeleteMessage() {
  return useMutation({
    mutationFn: async (id: string) =>
      unwrap(await api.DELETE("/teacher/messages/{id}", { params: { path: { id } } })),
  });
}
