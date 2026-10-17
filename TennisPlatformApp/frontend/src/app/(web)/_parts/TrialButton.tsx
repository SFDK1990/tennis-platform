import { trialHref } from "./school";

const LAYOUT = {
  /** The bar at the bottom of a phone. */
  wide: "flex w-full min-h-13 px-6 text-[17px]",
  /** In the page: on a phone only the bottom bar carries it, so no screen shows it twice. */
  desktop: "hidden lg:inline-flex min-h-13 px-6 text-[17px]",
  /** In the fixed top bar, where it stays at hand while scrolling on a computer. */
  bar: "hidden lg:inline-flex min-h-11 px-4 text-[15px]",
};

export function TrialButton({ layout }: { layout: keyof typeof LAYOUT }) {
  return (
    <a
      href={trialHref()}
      className={`${LAYOUT[layout]} items-center justify-center gap-2.5 rounded-lg bg-ball font-semibold text-navy hover:bg-(--site-band-text) hover:text-(--site-band)`}
    >
      <svg viewBox="0 0 24 24" aria-hidden className="size-5.5 fill-none stroke-current stroke-[1.8]">
        <path d="M20 12a8 8 0 0 1-11.6 7.1L4 20l1-4.2A8 8 0 1 1 20 12z" strokeLinejoin="round" />
      </svg>
      Pedir clase de prueba
    </a>
  );
}
