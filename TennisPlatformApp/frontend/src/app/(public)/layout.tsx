import Link from "next/link";
import type { ReactNode } from "react";
import { Logo } from "@/shared/ui/Logo";

/** The way in: the mark on navy, a court's baseline under it, and the form on white. */
export default function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-navy px-4 py-12">
      <div className="w-full max-w-md">
        <div className="relative mb-8 flex flex-col items-center gap-3 border-b-[3px] border-white/85 pb-6 text-white">
          <Logo className="h-16 w-16" onDark label="" />
          <p className="flex flex-col items-center">
            <span className="font-display text-3xl font-semibold leading-none">Oasis Papagayo</span>
            <span className="text-sm text-on-navy">Escuela de tenis</span>
          </p>
          {/* The centre mark of a baseline. */}
          <span aria-hidden className="absolute -bottom-[3px] left-1/2 h-3.5 w-[3px] -translate-x-1/2 bg-white/85" />
        </div>
        <div className="rounded-xl bg-paper p-6 sm:p-7">{children}</div>
        <nav aria-label="Legal" className="mt-6 flex justify-center gap-6 text-sm">
          <Link href="/aviso-legal" className="text-on-navy underline underline-offset-4 hover:text-white">Aviso legal</Link>
          <Link href="/privacidad" className="text-on-navy underline underline-offset-4 hover:text-white">Privacidad</Link>
        </nav>
      </div>
    </main>
  );
}
