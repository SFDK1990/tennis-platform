"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useSignOut, type Me } from "@/modules/identity/api";
import { AccountNotices } from "@/modules/identity/components/AccountNotices";
import { RequireAccount } from "@/modules/identity/components/RequireAccount";

const NAV: Record<string, { href: string; label: string }[]> = {
  STUDENT: [
    { href: "/calendar", label: "Clases" },
    { href: "/bookings", label: "Mis reservas" },
    { href: "/profile", label: "Perfil" },
  ],
  TEACHER: [
    { href: "/teacher", label: "Calendario" },
    { href: "/teacher/availability", label: "Horario" },
    { href: "/teacher/students", label: "Alumnos" },
    { href: "/profile", label: "Perfil" },
  ],
  ADMIN: [
    { href: "/admin", label: "Configuración" },
    { href: "/admin/users", label: "Usuarios" },
    { href: "/profile", label: "Perfil" },
  ],
};

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <RequireAccount>
      {(me) => (
        <>
          <Header me={me} />
          <main className="mx-auto flex max-w-3xl flex-col gap-6 px-4 py-8">
            <AccountNotices me={me} />
            {children}
          </main>
        </>
      )}
    </RequireAccount>
  );
}

function Header({ me }: { me: Me }) {
  const pathname = usePathname();
  const router = useRouter();
  const signOut = useSignOut();

  return (
    <header className="bg-court text-white">
      <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3">
        <Link href="/" className="font-display text-xl font-semibold">Tennis Platform</Link>
        <nav className="flex flex-wrap items-center gap-x-5 gap-y-1">
          {NAV[me.role].map((item) => (
            <Link key={item.href} href={item.href} aria-current={pathname === item.href ? "page" : undefined}
              className="border-b-2 border-transparent py-1 aria-[current=page]:border-ball">
              {item.label}
            </Link>
          ))}
          <button type="button" className="py-1 text-white/80 hover:text-white"
            onClick={() => signOut.mutate(undefined, { onSettled: () => router.replace("/login") })}>
            Salir
          </button>
        </nav>
      </div>
    </header>
  );
}
