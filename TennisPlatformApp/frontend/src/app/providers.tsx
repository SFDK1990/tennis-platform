"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { createQueryClient } from "@/shared/api/query";
import { restoreSession } from "@/shared/api/session";

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient);

  // Once per page load; restoreSession ignores a second call, which Strict Mode makes.
  useEffect(restoreSession, []);

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
