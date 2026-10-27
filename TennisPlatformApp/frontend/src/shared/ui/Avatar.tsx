/* eslint-disable @next/next/no-img-element -- a data URL of the person's own photo: there is nothing for next/image to optimise */

const SIZES = {
  sm: "size-10 text-sm",
  lg: "size-24 text-3xl",
} as const;

/** A person's photo, or their initials when there is none (Fase 25.5). The name next to it says who it is. */
export function Avatar({ name, src, size = "sm" }: { name: string | null; src?: string | null; size?: keyof typeof SIZES }) {
  if (src) {
    return <img src={src} alt="" className={`${SIZES[size]} shrink-0 rounded-full object-cover`} />;
  }
  return (
    <span aria-hidden className={`${SIZES[size]} flex shrink-0 items-center justify-center rounded-full bg-navy font-semibold text-white`}>
      {initialsOf(name)}
    </span>
  );
}

export function initialsOf(name: string | null): string {
  return (name ?? "").trim().split(/\s+/).slice(0, 2).map((word) => word[0]?.toUpperCase() ?? "").join("");
}
