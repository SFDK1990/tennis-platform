"use client";

import type { ReactNode } from "react";
import { RequireAccount } from "@/modules/identity/components/RequireAccount";

export default function AdminLayout({ children }: { children: ReactNode }) {
  return <RequireAccount roles={["ADMIN"]}>{() => children}</RequireAccount>;
}
