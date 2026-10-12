import Link from "next/link";
import { Logo } from "@/shared/ui/Logo";
import { AccountLink } from "./AccountLink";
import { school } from "./school";
import { ThemeToggle } from "./ThemeToggle";
import { TrialButton } from "./TrialButton";

export const WRAP = "mx-auto w-full max-w-6xl px-4 sm:px-10";

const SECTIONS = [
  ["/#clases", "Clases"],
  ["/#empezar", "Cómo empezar"],
  ["/#precios", "Precios"],
  ["/#preguntas", "Preguntas"],
] as const;

/**
 * The website's top bar, on the navy band. On a computer it stays at the top while scrolling,
 * with the trial button in it; a phone has the bar at the bottom for that, and the screen height
 * to spare is short. The section links need the width of a large screen to fit beside both
 * buttons.
 */
export function SiteNav() {
  return (
    <div className="bg-(--site-band) text-white lg:sticky lg:top-0 lg:z-20">
      <nav aria-label="Principal" className={`${WRAP} flex min-h-22 items-center gap-6`}>
        <Link href="/" className="flex shrink-0 items-center gap-3">
          <Logo className="size-11" onDark label="" />
          <span className="flex flex-col">
            <span className="font-display text-2xl font-bold leading-none">{school.name}</span>
            <span className="text-sm text-on-navy">Escuela de tenis</span>
          </span>
        </Link>
        <div className="ml-6 hidden gap-7 xl:flex">
          {SECTIONS.map(([href, label]) => (
            <a key={href} href={href} className="font-medium text-on-navy hover:text-white">{label}</a>
          ))}
        </div>
        <div className="ml-auto flex items-center gap-3">
          <ThemeToggle />
          <AccountLink className="inline-flex min-h-11 items-center rounded-lg border-2 border-white/70 px-4 text-[15px] font-semibold text-white hover:border-white hover:bg-white hover:text-navy" />
          <TrialButton layout="bar" />
        </div>
      </nav>
    </div>
  );
}

/**
 * On the home page a phone keeps the trial bar fixed at the bottom, so the footer leaves room
 * for it; the other pages have no bar.
 */
export function SiteFooter({ underTrialBar = false }: { underTrialBar?: boolean }) {
  return (
    <footer className={`${WRAP} flex flex-wrap items-center gap-x-6 gap-y-3 pt-9 text-[15px] ${underTrialBar ? "pb-28 lg:pb-9" : "pb-9"}`}>
      <Logo className="size-9" label={school.name} />
      <span className="text-(--site-muted)">© {school.name}, escuela de tenis</span>
      <span className="flex flex-wrap gap-x-6 gap-y-2 sm:ml-auto">
        <Link href="/aviso-legal" className="text-(--site-link) underline-offset-4 hover:underline">Aviso legal</Link>
        <Link href="/privacidad" className="text-(--site-link) underline-offset-4 hover:underline">Privacidad</Link>
        <Link href="/login" className="text-(--site-link) underline-offset-4 hover:underline">Entrar en la app</Link>
      </span>
    </footer>
  );
}
