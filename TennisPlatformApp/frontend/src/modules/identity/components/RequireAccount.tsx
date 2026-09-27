"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { homeFor, useMe, useSession, type Me, type Role } from "@/modules/identity/api";
import { ErrorNotice } from "@/shared/ui/Notice";

/**
 * Keeps a screen to the roles it is for. It is a convenience, not the protection: the backend
 * checks every call again, and that is the check that counts.
 */
export function RequireAccount({ roles, children }: { roles?: Role[]; children: (me: Me) => ReactNode }) {
  const router = useRouter();
  const { status } = useSession();
  const me = useMe();
  const role = me.data?.role;
  const allowed = role !== undefined && (!roles || roles.includes(role));

  useEffect(() => {
    if (status === "signed-out") {
      router.replace("/login");
    } else if (role !== undefined && !allowed) {
      router.replace(homeFor(role));
    }
  }, [status, role, allowed, router]);

  if (me.error) {
    return <ErrorNotice error={me.error} />;
  }
  if (!allowed || !me.data) {
    return <p className="py-12 text-center text-muted">Cargando…</p>;
  }
  return children(me.data);
}
