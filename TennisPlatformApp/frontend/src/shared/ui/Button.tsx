import type { ButtonHTMLAttributes } from "react";

export type ButtonVariant = "primary" | "secondary" | "quiet" | "danger" | "onCourt" | "outlineOnCourt";

/** Also used by links styled as buttons, so both read the same. */
export const BUTTON_BASE =
  "inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50";

export const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary: "bg-court text-white hover:bg-court-deep",
  secondary: "border-[1.5px] border-navy bg-paper text-navy hover:bg-court-tint",
  quiet: "border border-line bg-paper text-ink hover:border-court",
  danger: "border border-fault/40 bg-paper text-fault hover:bg-fault hover:text-white",
  // On the court-blue cards, where the page's own buttons would disappear.
  onCourt: "bg-paper text-court hover:bg-court-tint",
  outlineOnCourt: "border-[1.5px] border-white/85 text-white hover:bg-white/10",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  pending?: boolean;
}

export function Button({ variant = "primary", pending = false, disabled, className = "", children, ...rest }: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || pending}
      aria-busy={pending || undefined}
      className={`${BUTTON_BASE} ${BUTTON_VARIANTS[variant]} ${className}`}
    >
      {children}
    </button>
  );
}
