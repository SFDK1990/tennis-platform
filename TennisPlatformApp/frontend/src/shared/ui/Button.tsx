import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "book" | "quiet" | "danger";

const VARIANTS: Record<Variant, string> = {
  primary: "bg-court text-white hover:bg-court-deep",
  // The ball colour: kept for taking a seat, and nothing else.
  book: "bg-ball text-ink hover:brightness-95",
  quiet: "border border-line bg-paper text-ink hover:border-court",
  danger: "border border-fault/40 bg-paper text-fault hover:bg-fault hover:text-white",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  pending?: boolean;
}

export function Button({ variant = "primary", pending = false, disabled, className = "", children, ...rest }: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || pending}
      aria-busy={pending || undefined}
      className={`inline-flex min-h-10 items-center justify-center rounded-md px-4 font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${VARIANTS[variant]} ${className}`}
    >
      {children}
    </button>
  );
}
