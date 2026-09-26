"use client";

import type { ReactNode } from "react";
import { RequireAccount } from "@/modules/identity/components/RequireAccount";

export default function TeacherLayout({ children }: { children: ReactNode }) {
  return <RequireAccount roles={["TEACHER"]}>{() => children}</RequireAccount>;
}
