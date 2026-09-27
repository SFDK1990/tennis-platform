import { useId, type InputHTMLAttributes, type ReactNode } from "react";

/** The look of every text box and select, so a select in a module matches a Field. */
export const CONTROL_CLASS =
  "min-h-11 rounded-lg border-[1.5px] border-line bg-paper px-3 font-normal hover:border-muted aria-invalid:border-fault";

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  hint?: ReactNode;
  error?: string;
}

/** A labelled input whose error is announced with it (aria-describedby). */
export function Field({ label, hint, error, className = "", ...input }: FieldProps) {
  const id = useId();
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;
  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      <label htmlFor={id} className="text-sm font-semibold">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={CONTROL_CLASS}
        {...input}
      />
      {error ? (
        <p id={`${id}-error`} className="text-sm text-fault">{error}</p>
      ) : hint ? (
        <p id={`${id}-hint`} className="text-sm text-muted">{hint}</p>
      ) : null}
    </div>
  );
}
