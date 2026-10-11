"use client";

import Link from "next/link";
import { homeFor, useMe, useSession } from "@/modules/identity/api";

/**
 * "Entrar" until the session comes back, then "Mi cuenta". The server cannot tell which while
 * it renders the page (the refresh cookie only travels to /api/v1/auth), so nobody is
 * redirected away from the website: the link changes instead (31-fase31, decision 2).
 */
export function AccountLink({ className }: { className?: string }) {
  const { status } = useSession();
  const role = useMe().data?.role;
  return status === "signed-in" && role ? (
    <Link href={homeFor(role)} className={className}>Mi cuenta</Link>
  ) : (
    <Link href="/login" className={className}>Entrar</Link>
  );
}
