"use client";

import { UsersList } from "@/modules/administration/components/UsersList";
import { useTeacherZone } from "@/modules/teacher/api";

export default function AdminUsersPage() {
  const zone = useTeacherZone() ?? "Europe/Madrid";
  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Usuarios</h1>
      <UsersList zone={zone} />
    </>
  );
}
