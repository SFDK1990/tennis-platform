"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import type { Me, Role } from "@/modules/identity/api";
import { AccountNotices } from "@/modules/identity/components/AccountNotices";
import { RequireAccount } from "@/modules/identity/components/RequireAccount";
import { SignOutButton } from "@/modules/identity/components/SignOutButton";
import { Icon, type IconName } from "@/shared/ui/Icon";
import { Logo } from "@/shared/ui/Logo";

interface NavItem {
  href: string;
  label: string;
  icon: IconName;
  /** Deeper screens that still belong to this item, such as a lesson under the agenda. */
  also?: string[];
}

const NAV: Record<Role, NavItem[]> = {
  STUDENT: [
    { href: "/home", label: "Inicio", icon: "home" },
    { href: "/calendar", label: "Clases", icon: "calendar" },
    { href: "/bookings", label: "Mis reservas", icon: "ticket" },
    { href: "/profile", label: "Perfil", icon: "user" },
  ],
  TEACHER: [
    { href: "/teacher", label: "Hoy", icon: "today" },
    { href: "/teacher/calendar", label: "Agenda", icon: "calendar", also: ["/teacher/lessons/"] },
    { href: "/teacher/students", label: "Alumnos", icon: "users" },
    { href: "/teacher/availability", label: "Horario", icon: "clock" },
    { href: "/profile", label: "Perfil", icon: "user" },
  ],
  ADMIN: [
    { href: "/admin", label: "Configuración", icon: "settings" },
    { href: "/admin/lessons", label: "Clases", icon: "calendar", also: ["/admin/lessons/"] },
    { href: "/admin/users", label: "Usuarios", icon: "users" },
    { href: "/profile", label: "Perfil", icon: "user" },
  ],
};

/** The student's home says what is missing as its own first steps; elsewhere it is a notice. */
const SCREENS_WITH_THEIR_OWN_NOTICES = ["/home"];

/**
 * One frame for every size (27-fase15.5, decision 3): below 1024 px a top bar and a tab bar at
 * the bottom, from 1024 px a navy sidebar. Only one navigation is ever displayed, so the page
 * never has two with the same name.
 */
export default function AppLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  return (
    <RequireAccount>
      {(me) => {
        const items = NAV[me.role];
        const current = items.find((item) => isCurrent(item, pathname))?.href;
        return (
          <div className="min-h-screen">
            <Sidebar me={me} items={items} current={current} />
            <div className="flex min-h-screen flex-col lg:pl-64">
              <header className="flex items-center px-5 pb-1 pt-4 lg:hidden">
                <Link href="/" className="text-court" aria-label="Inicio"><Logo className="h-7 w-auto" /></Link>
              </header>
              <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 px-4 pb-28 pt-3 sm:px-6 lg:px-10 lg:pb-12 lg:pt-10">
                {SCREENS_WITH_THEIR_OWN_NOTICES.includes(pathname) ? null : <AccountNotices me={me} />}
                {children}
              </main>
            </div>
            <TabBar items={items} current={current} />
          </div>
        );
      }}
    </RequireAccount>
  );
}

function isCurrent(item: NavItem, pathname: string): boolean {
  return pathname === item.href || (item.also ?? []).some((prefix) => pathname.startsWith(prefix));
}

function Sidebar({ me, items, current }: { me: Me; items: NavItem[]; current?: string }) {
  return (
    <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col bg-navy px-3.5 py-6 text-white lg:flex">
      <Link href="/" className="mb-7 flex items-center gap-3 px-2.5">
        <Logo className="h-8 w-auto" />
        <span className="font-display text-lg font-medium leading-none">Tennis<br />Academy</span>
      </Link>
      <nav aria-label="Principal" className="flex flex-col gap-1">
        {items.map((item) => (
          <Link key={item.href} href={item.href} aria-current={item.href === current ? "page" : undefined}
            className="flex min-h-11 items-center gap-3 rounded-lg px-3.5 font-medium text-on-navy hover:bg-white/5 hover:text-white aria-[current=page]:bg-white/10 aria-[current=page]:font-semibold aria-[current=page]:text-white">
            <Icon name={item.icon} />
            {item.label}
          </Link>
        ))}
      </nav>
      <div className="mt-auto flex flex-col gap-3 border-t border-white/15 px-2.5 pt-4">
        <p className="text-sm leading-tight">
          <span className="block font-semibold">{me.displayName ?? me.fullName ?? me.email}</span>
          <span className="text-on-navy">{ROLE_NAME[me.role]}</span>
        </p>
        <SignOutButton className="flex min-h-11 items-center gap-3 rounded-lg px-1 text-on-navy hover:text-white" />
      </div>
    </aside>
  );
}

const ROLE_NAME: Record<Role, string> = { STUDENT: "Alumno", TEACHER: "Profesor", ADMIN: "Administración" };

function TabBar({ items, current }: { items: NavItem[]; current?: string }) {
  return (
    <nav aria-label="Principal"
      className="fixed inset-x-0 bottom-0 z-10 flex border-t border-line bg-paper px-1 pb-[max(env(safe-area-inset-bottom),0.5rem)] lg:hidden">
      {items.map((item) => (
        <Link key={item.href} href={item.href} aria-current={item.href === current ? "page" : undefined}
          className="flex min-h-14 flex-1 flex-col items-center justify-center gap-0.5 text-xs font-semibold text-muted aria-[current=page]:text-court">
          <Icon name={item.icon} className="size-6" />
          {item.label}
        </Link>
      ))}
    </nav>
  );
}
