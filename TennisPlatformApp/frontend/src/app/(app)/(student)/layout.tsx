"use client";

import type { ReactNode } from "react";
import { RequireAccount } from "@/modules/identity/components/RequireAccount";

export default function StudentLayout({ children }: { children: ReactNode }) {
  return <RequireAccount roles={["STUDENT"]}>{() => children}</RequireAccount>;
}
